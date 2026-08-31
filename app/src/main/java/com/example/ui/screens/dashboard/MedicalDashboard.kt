package com.example.ui.screens.dashboard

import android.widget.Toast
import androidx.compose.animation.*
import com.example.viewmodels.StoreViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*

import com.example.utils.VisualThemePalette

import com.example.data.repositories.*

/**
 * 🏥 Standalone Dedicated Dashboard for Medical Centers & Clinics (لوحة المركز الطبي والعيادات)
 */
@Composable
fun MedicalDashboard(
    account: UnifiedBusinessAccount,
    storeViewModel: StoreViewModel = viewModel(),
    themeColors: VisualThemePalette,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    var activeTab by remember { mutableIntStateOf(0) }

    val medicalViewModel = remember(account.id) {
        MedicalDashboardViewModel(
            centerId = account.id,
            dashboardRepository = DashboardRepositoryImpl(context)
        )
    }

    val medicalUiState by medicalViewModel.uiState.collectAsState()

    LaunchedEffect(medicalViewModel) {
        medicalViewModel.eventFlow.collect { event ->
            when (event) {
                is DashboardEvent.ShowToast -> Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                is DashboardEvent.NavigateToDetail -> { }
            }
        }
    }

    val tabsList = listOf(
        Pair("🩺", "العيادات والخدمات"),
        Pair("👨‍⚕️", "كادر الأطباء"),
        Pair("📅", "الحجوزات الطبية"),
        Pair("💬", "تقييمات المرضى"),
        Pair("📝", "الملف الطبي للمركز"),
        Pair("📊", "الإحصائيات والأداء")
    )

    val stores by storeViewModel.stores.collectAsState()
    val matchingStore = stores.find { it.id == account.id || it.phone == account.phone }
    val isVerified = account.isVerified || (matchingStore?.isActive == true)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A)) // Custom Warm Navy Theme
    ) {
        // Top Header Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1E293B))
                .border(1.dp, Color.White.copy(alpha = 0.08f))
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier
                    .size(36.dp)
                    .background(Color.White.copy(alpha = 0.08f), CircleShape)
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "رجوع", tint = Color.White)
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = account.name.ifBlank { "لوحة تحكم المركز الطبي" },
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "🏥 مركز طبي وعيادات • ${account.neighborhood.ifBlank { account.city }}",
                    fontSize = 11.sp,
                    color = themeColors.accent
                )
            }

            Surface(
                color = if (isVerified) Color(0xFF10B981) else Color(0xFFF59E0B),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = if (isVerified) "موثق ✓" else "قيد التوثيق ⏳",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    fontSize = 10.sp,
                    color = Color.Black,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Horizontal Tabs Bar
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0F172A))
                .padding(vertical = 10.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(tabsList.size) { index ->
                val tab = tabsList[index]
                val isSelected = activeTab == index
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isSelected) themeColors.accent else Color(0xFF1E293B))
                        .border(
                            1.dp,
                            if (isSelected) themeColors.accent else Color.White.copy(alpha = 0.08f),
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
                            color = if (isSelected) Color.Black else Color.White,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }

        HorizontalDivider(color = Color.White.copy(alpha = 0.08f), thickness = 1.dp)

        // Dynamic Tab Content
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(12.dp)
        ) {
            when (activeTab) {
                0 -> TabProductsServices(account, viewModel, themeColors)
                1 -> MedicalDoctorsSection(account, medicalViewModel, themeColors)
                2 -> TabBookingsOrders(account, viewModel, themeColors)
                3 -> TabReviewsFeedback(account, viewModel, themeColors)
                4 -> TabProfileEdit(account, viewModel, themeColors)
                5 -> TabStatisticsGrowth(account, viewModel, themeColors)
            }
        }
    }
}

// ==========================================================
// 👨‍⚕️ Custom Doctor Management Section
// ==========================================================
@Composable
private fun MedicalDoctorsSection(
    account: UnifiedBusinessAccount,
    medicalViewModel: MedicalDashboardViewModel,
    themeColors: VisualThemePalette
) {
    val context = LocalContext.current
    var showAddDialog by remember { mutableStateOf(false) }
    var doctorName by remember { mutableStateOf("") }
    var doctorSpecialty by remember { mutableStateOf("") }
    var doctorHours by remember { mutableStateOf("") }

    val doctorsList by medicalViewModel.doctors.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "👨‍⚕️ كادر الأطباء والاستشاريين بالمركز (${doctorsList.size})",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = themeColors.accent
            )
            Button(
                onClick = {
                    doctorName = ""
                    doctorSpecialty = ""
                    doctorHours = ""
                    showAddDialog = true
                },
                colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("إضافة طبيب 👨‍⚕️", fontSize = 10.5.sp, color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }

        if (doctorsList.isEmpty()) {
            UnifiedEmptyState(
                icon = "👨‍⚕️",
                title = "لا يوجد أطباء مسجلين",
                description = "يمكنك إضافة كادر الأطباء ومواعيد عياداتهم المخصصة ليسهل حجز المرضى لها.",
                themeColors = themeColors
            )
        } else {
            doctorsList.forEach { doc ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("🩺 ${doc.name}", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(doc.specialty, fontSize = 11.sp, color = themeColors.accent)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(doc.hours, fontSize = 10.sp, color = Color.LightGray)
                        }
                        IconButton(
                            onClick = {
                                medicalViewModel.deleteDoctor(doc.id)
                            }
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "حذف", tint = Color(0xFFEF5350))
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("إضافة طبيب جديد للكادر 👨‍⚕️", fontSize = 14.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = doctorName,
                        onValueChange = { doctorName = it },
                        label = { Text("اسم الطبيب بالكامل", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = doctorSpecialty,
                        onValueChange = { doctorSpecialty = it },
                        label = { Text("التخصص الطبي الدقيق", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = doctorHours,
                        onValueChange = { doctorHours = it },
                        label = { Text("أوقات الدوام وساعات العيادة", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (doctorName.isNotBlank() && doctorSpecialty.isNotBlank()) {
                            medicalViewModel.addDoctor(doctorName, doctorSpecialty, doctorHours)
                            showAddDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent)
                ) {
                    Text("إضافة الطبيب ✓", fontSize = 11.sp, color = Color.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("إلغاء", fontSize = 11.sp, color = Color.LightGray)
                }
            }
        )
    }
}
