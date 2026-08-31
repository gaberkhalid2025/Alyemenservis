package com.example.ui.screens.home.sections

import androidx.compose.foundation.BorderStroke
import com.example.viewmodels.JobViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.JobEntity

import com.example.utils.VisualThemePalette

/**
 * 💼 JobsSectionView - عرض إعلانات الوظائف والفرص المتاحة فقط
 */
@Composable
fun JobsSectionView(
    jobViewModel: JobViewModel = viewModel(),
    themeColors: VisualThemePalette,
    onJobClick: (JobEntity) -> Unit,
    onCreateJobClick: () -> Unit
) {
    val jobsList by jobViewModel.jobs.collectAsState()
    val activeJobs = remember(jobsList) { jobsList.filter { !it.isDeleted && (it.isApproved || it.isActive) } }
    var selectedSubCategory by remember { mutableStateOf("الكل") }

    val subCategories = listOf(
        "الكل",
        "📊 وظائف إدارية",
        "💻 هندسة وتقنية",
        "📢 تسويق ومبيعات",
        "👨‍🏫 تدريس وتعليم",
        "🛡️ حراسة وخدمات",
        "🛠️ مهن وحرف"
    )

    val filteredList = remember(activeJobs, selectedSubCategory) {
        if (selectedSubCategory == "الكل") activeJobs
        else {
            val key = selectedSubCategory.substringAfter(" ").trim()
            activeJobs.filter { 
                it.title.contains(key) || it.companyName.contains(key) || it.jobType.contains(key)
            }
        }
    }

    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("💼 إعلانات الوظائـف الشاغرة:", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text("تصفح أحدث فرص العمل والوظائف الشاغرة باليمن", fontSize = 10.sp, color = Color.Gray)
            }
            Button(
                onClick = onCreateJobClick,
                colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("نشر إعلان وظيفة", fontSize = 10.5.sp, color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }

        // Subcategories row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            subCategories.forEach { subCat ->
                val isSelected = selectedSubCategory == subCat
                Surface(
                    onClick = { selectedSubCategory = subCat },
                    shape = RoundedCornerShape(16.dp),
                    color = if (isSelected) themeColors.accent else Color(0xFF1E293B),
                    border = BorderStroke(1.dp, if (isSelected) themeColors.accent else Color.White.copy(alpha = 0.1f))
                ) {
                    Text(
                        subCat,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) Color.Black else Color.White,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }

        if (filteredList.isEmpty()) {
            Card(colors = CardDefaults.cardColors(containerColor = themeColors.surface), modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Text("لا توجد إعلانات وظائف معروضة في هذا القسم حالياً.", fontSize = 11.sp, color = Color.Gray, modifier = Modifier.padding(16.dp), textAlign = TextAlign.Center)
            }
        } else {
            filteredList.forEach { job ->
                Card(
                    onClick = { onJobClick(job) },
                    colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(0.8.dp, themeColors.accent.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF1E293B),
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("👨‍💼", fontSize = 22.sp)
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(job.title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text("🏢 ${job.companyName} - 📍 ${job.cityId}", fontSize = 10.5.sp, color = Color.LightGray)
                            if (job.salary.isNotEmpty()) {
                                Text("💰 الراتب: ${job.salary}", fontSize = 10.sp, color = themeColors.accent)
                            }
                        }
                    }
                }
            }
        }
    }
}
