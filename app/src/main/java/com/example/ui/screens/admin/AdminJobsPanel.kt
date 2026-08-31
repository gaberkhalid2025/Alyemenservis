package com.example.ui.screens.admin
import com.example.ui.MainViewModel

import androidx.compose.foundation.BorderStroke
import com.example.viewmodels.JobViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
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

    var selectedFilter by remember { mutableStateOf("الكل") }
    val filters = listOf("الكل", "مميز VIP", "نشط")

    val filteredJobs = remember(jobs, selectedFilter) {
        jobs.filter { job ->
            when (selectedFilter) {
                "مميز VIP" -> job.isVip
                "نشط" -> !job.isDeleted
                else -> true
            }
        }
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
            AdminFilterChips(
                categories = filters,
                selectedCategory = selectedFilter,
                onSelectCategory = { selectedFilter = it },
                themeColors = themeColors
            )

            if (filteredJobs.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("لا توجد إعلانات وظائف حالياً", color = Color.Gray, fontSize = 13.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredJobs, key = { it.id }) { job ->
                        AdminEntityCard(
                            title = job.title.ifBlank { "فرصة عمل" },
                            subtitle = "🏢 ${job.companyName} • 📍 ${job.cityId} • 💰 ${job.salary}",
                            details = "📝 ${job.description.take(60)}...",
                            statusText = if (job.isVip) "VIP ⭐" else "عادي",
                            statusColor = if (job.isVip) Color(0xFFF59E0B) else Color(0xFF10B981),
                            isVip = job.isVip,
                            themeColors = themeColors,
                            actions = {
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
                        )
                    }
                }
            }
        }
    }
}
