package com.example.ui.screens.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.PendingProviderEntity
import com.example.ui.MainViewModel
import com.example.utils.VisualThemePalette

/**
 * 📨 AdminRequestsPanel
 * إدارة ومراجعة كافة طلبات الانضمام والتوثيق والاعتماد المعلقة لجميع القطاعات
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminRequestsPanel(
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val pendingProviders by viewModel.pendingProviders.collectAsState()
    val properties by viewModel.properties.collectAsState()
    val stores by viewModel.stores.collectAsState()
    val jobs by viewModel.jobs.collectAsState()
    val registeredUsersList by viewModel.registeredUsersList.collectAsState()

    var selectedTab by remember { mutableStateOf("SERVICES") }
    var rejectingRequest by remember { mutableStateOf<PendingProviderEntity?>(null) }
    var rejectionReason by remember { mutableStateOf("") }

    val pendingPropertiesCount = properties.count { !it.isApproved && !it.isDeleted }
    val pendingMedicalCount = stores.count { !it.isApproved && !it.isDeleted && (it.categoryId.contains("طبي") || it.categoryId.contains("عياد")) }
    val pendingRestaurantsCount = stores.count { !it.isApproved && !it.isDeleted && (it.categoryId.contains("مطعم") || it.categoryId.contains("كافيه")) }
    val pendingStoresCount = stores.count { !it.isApproved && !it.isDeleted && !it.categoryId.contains("طبي") && !it.categoryId.contains("عياد") && !it.categoryId.contains("مطعم") && !it.categoryId.contains("كافيه") }
    val pendingJobsCount = jobs.count { !it.isApproved && !it.isDeleted }

    val activeServicesCount = pendingProviders.count {
        (it.status == "PENDING" || it.status.isEmpty()) &&
        it.categoryId != "STORE" && it.categoryId != "RESTAURANT" &&
        it.categoryId != "MEDICAL" && it.categoryId != "PROPERTY" &&
        it.categoryId != "JOB" && it.categoryId != "CLIENT"
    }

    val subTabs = listOf(
        Triple("SERVICES", "🔧 المهن والخدمات", activeServicesCount),
        Triple("PROPERTIES", "🏠 العقارات", pendingPropertiesCount),
        Triple("STORES", "🏪 المحلات", pendingStoresCount),
        Triple("MEDICAL", "🏥 المراكز الطبية", pendingMedicalCount),
        Triple("RESTAURANTS", "🍔 المطاعم", pendingRestaurantsCount),
        Triple("JOBS", "💼 إعلانات التوظيف", pendingJobsCount),
        Triple("USERS", "👤 المستخدمين", registeredUsersList.size)
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("📨 طلبات الانضمام والاعتماد المعلقة", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = themeColors.accent)
            if (onBack != {}) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع", tint = Color.White)
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(subTabs) { st ->
                val isSel = selectedTab == st.first
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSel) themeColors.accent else themeColors.surface)
                        .clickable { selectedTab = st.first }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .border(1.dp, if (isSel) Color.White else Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                ) {
                    Text("${st.second} (${st.third})", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isSel) Color.Black else Color.White)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        when (selectedTab) {
            "SERVICES" -> {
                val activePending = pendingProviders.filter {
                    (it.status == "PENDING" || it.status.isEmpty()) &&
                    it.categoryId != "STORE" && it.categoryId != "RESTAURANT" &&
                    it.categoryId != "MEDICAL" && it.categoryId != "PROPERTY" &&
                    it.categoryId != "JOB" && it.categoryId != "CLIENT"
                }
                if (activePending.isEmpty()) {
                    Card(colors = CardDefaults.cardColors(containerColor = themeColors.surface), modifier = Modifier.fillMaxWidth()) {
                        Text("لا توجد طلبات معلقة للمهن والخدمات حالياً.", fontSize = 12.sp, color = themeColors.textSecondary, modifier = Modifier.padding(16.dp))
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        activePending.forEach { req ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                                border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(text = "الاسم: ${req.name}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    Text(text = "الهاتف: ${req.phone}", fontSize = 11.sp, color = themeColors.textSecondary)
                                    Text(text = "المنطقة: ${req.area} - ${req.localNeighborhood}", fontSize = 11.sp, color = themeColors.textSecondary)
                                    if (req.password.isNotBlank()) {
                                        Text(text = "🔑 كلمة المرور: ${req.password}", fontSize = 11.sp, color = themeColors.accent, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                        Button(
                                            onClick = { viewModel.approveRequest(req) },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("موافقة واعتماد", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                        }
                                        Button(
                                            onClick = { rejectingRequest = req },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("رفض الطلب", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            "PROPERTIES" -> {
                val pendingProps = properties.filter { !it.isApproved && !it.isDeleted }
                if (pendingProps.isEmpty()) {
                    Card(colors = CardDefaults.cardColors(containerColor = themeColors.surface), modifier = Modifier.fillMaxWidth()) {
                        Text("لا توجد عقارات بانتظار الموافقة حالياً.", fontSize = 12.sp, color = themeColors.textSecondary, modifier = Modifier.padding(16.dp))
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        pendingProps.forEach { prop ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                                border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(text = prop.title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    Text(text = "الموقع: ${prop.cityId} - ${prop.localNeighborhood}", fontSize = 11.sp, color = themeColors.textSecondary)
                                    Text(text = "السعر: ${prop.price} | الهاتف: ${prop.phone}", fontSize = 11.sp, color = themeColors.textSecondary)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                        Button(
                                            onClick = { viewModel.setPropertyActive(prop.id, true) },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("موافقة ونشر", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                        }
                                        Button(
                                            onClick = { viewModel.deleteProperty(prop.id) },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("حذف / رفض", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            "STORES", "MEDICAL", "RESTAURANTS" -> {
                val filteredStores = stores.filter { s ->
                    !s.isApproved && !s.isDeleted && when (selectedTab) {
                        "MEDICAL" -> s.categoryId.contains("طبي") || s.categoryId.contains("عياد")
                        "RESTAURANTS" -> s.categoryId.contains("مطعم") || s.categoryId.contains("كافيه")
                        else -> !s.categoryId.contains("طبي") && !s.categoryId.contains("عياد") && !s.categoryId.contains("مطعم") && !s.categoryId.contains("كافيه")
                    }
                }
                if (filteredStores.isEmpty()) {
                    Card(colors = CardDefaults.cardColors(containerColor = themeColors.surface), modifier = Modifier.fillMaxWidth()) {
                        Text("لا توجد طلبات معلقة لهذا القسم حالياً.", fontSize = 12.sp, color = themeColors.textSecondary, modifier = Modifier.padding(16.dp))
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        filteredStores.forEach { s ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                                border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(text = s.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    Text(text = "التصنيف: ${s.categoryId} | الهاتف: ${s.phone}", fontSize = 11.sp, color = themeColors.textSecondary)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                        Button(
                                            onClick = { viewModel.setStoreActive(s.id, true) },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("موافقة واعتماد", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                        }
                                        Button(
                                            onClick = { viewModel.deleteStore(s.id) },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("رفض وحذف", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            "JOBS" -> {
                val pendingJobs = jobs.filter { !it.isApproved && !it.isDeleted }
                if (pendingJobs.isEmpty()) {
                    Card(colors = CardDefaults.cardColors(containerColor = themeColors.surface), modifier = Modifier.fillMaxWidth()) {
                        Text("لا توجد إعلانات وظائف معلقة بانتظار الاعتماد.", fontSize = 12.sp, color = themeColors.textSecondary, modifier = Modifier.padding(16.dp))
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        pendingJobs.forEach { job ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = themeColors.surface),
                                border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(text = job.title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    Text(text = "جهة العمل: ${job.companyName} | الموقع: ${job.cityId}", fontSize = 11.sp, color = themeColors.textSecondary)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                        Button(
                                            onClick = { viewModel.setJobApproved(job.id, true) },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("موافقة ونشر", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                        }
                                        Button(
                                            onClick = { viewModel.deleteJob(job.id) },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("رفض وحذف", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            "USERS" -> {
                AdminUsersPanel(viewModel = viewModel, themeColors = themeColors)
            }
        }
    }

    rejectingRequest?.let { req ->
        AlertDialog(
            onDismissRequest = { rejectingRequest = null },
            title = { Text("رفض طلب الانضمام", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("يرجى كتابة سبب الرفض لإشعار مقدم الطلب:", color = themeColors.textSecondary, fontSize = 12.sp)
                    OutlinedTextField(
                        value = rejectionReason,
                        onValueChange = { rejectionReason = it },
                        label = { Text("سبب الرفض...") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.rejectRequest(req, rejectionReason)
                        rejectingRequest = null
                        rejectionReason = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("تأكيد الرفض", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { rejectingRequest = null }) {
                    Text("إلغاء", color = Color.White)
                }
            }
        )
    }
}
