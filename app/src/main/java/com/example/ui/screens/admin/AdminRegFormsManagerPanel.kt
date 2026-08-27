package com.example.ui.screens.admin

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import java.text.SimpleDateFormat
import java.util.*

/**
 * 📝 AdminRegFormsManagerPanel
 * إدارة ومراجعة استمارات وطلبات التسجيل والانضمام لمقدمي الخدمات والمتاجر
 */
data class RegApplicationItem(
    val id: String,
    val applicantName: String,
    val serviceType: String,
    val phone: String,
    val city: String,
    val neighborhood: String,
    val nationalId: String,
    val experienceYears: Int,
    val status: String = "PENDING", // PENDING, APPROVED, REJECTED
    val submissionDate: Long = System.currentTimeMillis(),
    val notes: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminRegFormsManagerPanel(
    onBack: () -> Unit = {},
    adminViewModel: AdminViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedFilter by remember { mutableStateOf("ALL") }
    var searchQuery by remember { mutableStateOf("") }
    var selectedAppForReview by remember { mutableStateOf<RegApplicationItem?>(null) }
    var rejectReason by remember { mutableStateOf("") }
    var showRejectDialog by remember { mutableStateOf(false) }

    var applicationsList by remember {
        mutableStateOf(
            listOf(
                RegApplicationItem(
                    id = "REG-901",
                    applicantName = "م. خالد عبدالله الأهدل",
                    serviceType = "هندسة طاقة شمسية وكهرباء",
                    phone = "771234567",
                    city = "صنعاء",
                    neighborhood = "حدة",
                    nationalId = "010100998877",
                    experienceYears = 6,
                    status = "PENDING",
                    submissionDate = System.currentTimeMillis() - 1000L * 60 * 60 * 5
                ),
                RegApplicationItem(
                    id = "REG-902",
                    applicantName = "ورشة الفارس لصيانة التكييف",
                    serviceType = "صيانة مكيفات وتبريد",
                    phone = "733445566",
                    city = "عدن",
                    neighborhood = "المنصورة",
                    nationalId = "020200554433",
                    experienceYears = 8,
                    status = "PENDING",
                    submissionDate = System.currentTimeMillis() - 1000L * 60 * 60 * 24
                ),
                RegApplicationItem(
                    id = "REG-903",
                    applicantName = "صيدلية الشفاء الحديثة",
                    serviceType = "منشأة طبية / صيدلية",
                    phone = "711889900",
                    city = "تعز",
                    neighborhood = "شارع جمال",
                    nationalId = "030300112233",
                    experienceYears = 10,
                    status = "APPROVED",
                    submissionDate = System.currentTimeMillis() - 1000L * 60 * 60 * 72
                ),
                RegApplicationItem(
                    id = "REG-904",
                    applicantName = "نجارة الماهر للأثاث",
                    serviceType = "أعمال نجارة وديكور",
                    phone = "775566778",
                    city = "إب",
                    neighborhood = "المشنة",
                    nationalId = "040400887766",
                    experienceYears = 3,
                    status = "REJECTED",
                    submissionDate = System.currentTimeMillis() - 1000L * 60 * 60 * 96,
                    notes = "عدم استكمال صور الهوية وسجل النشاط"
                )
            )
        )
    }

    val filteredList = remember(applicationsList, selectedFilter, searchQuery) {
        applicationsList.filter { item ->
            val matchesFilter = when (selectedFilter) {
                "PENDING" -> item.status == "PENDING"
                "APPROVED" -> item.status == "APPROVED"
                "REJECTED" -> item.status == "REJECTED"
                else -> true
            }
            val matchesSearch = searchQuery.isBlank() ||
                    item.applicantName.contains(searchQuery, ignoreCase = true) ||
                    item.serviceType.contains(searchQuery, ignoreCase = true) ||
                    item.phone.contains(searchQuery, ignoreCase = true) ||
                    item.city.contains(searchQuery, ignoreCase = true)
            matchesFilter && matchesSearch
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("إدارة استمارات وطلبات التسجيل", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        Text("طلبات الانضمام: ${applicationsList.size}", fontSize = 11.sp, color = Color.Gray)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        Toast.makeText(context, "تم تحديث قائمة استمارات التسجيل", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "تحديث", tint = Color(0xFF00668B))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF8FAFC))
        ) {
            // شريط البحث
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("بحث باسم المتقدم، المهنة، المدينة، أو الهاتف...", fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF64748B)) },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "مسح")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF00668B),
                    unfocusedBorderColor = Color(0xFFCBD5E1)
                )
            )

            // شريط الفلاتر
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val filters = listOf(
                    "ALL" to "الكل (${applicationsList.size})",
                    "PENDING" to "قيد المراجعة ⏳ (${applicationsList.count { it.status == "PENDING" }})",
                    "APPROVED" to "المقبولة ✅ (${applicationsList.count { it.status == "APPROVED" }})",
                    "REJECTED" to "المرفوضة ❌ (${applicationsList.count { it.status == "REJECTED" }})"
                )
                items(filters) { (key, label) ->
                    val isSelected = selectedFilter == key
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedFilter = key },
                        label = { Text(label, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF00668B),
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // قائمة الاستمارات
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredList, key = { it.id }) { item ->
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .background(
                                                when (item.status) {
                                                    "APPROVED" -> Color(0xFFE8F5E9)
                                                    "REJECTED" -> Color(0xFFFFEBEE)
                                                    else -> Color(0xFFFFF3E0)
                                                },
                                                CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            when (item.status) {
                                                "APPROVED" -> Icons.Default.CheckCircle
                                                "REJECTED" -> Icons.Default.Close
                                                else -> Icons.Default.Info
                                            },
                                            contentDescription = null,
                                            tint = when (item.status) {
                                                "APPROVED" -> Color(0xFF2E7D32)
                                                "REJECTED" -> Color(0xFFD32F2F)
                                                else -> Color(0xFFE65100)
                                            },
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Column {
                                        Text(item.applicantName, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF1E293B))
                                        Text(item.serviceType, fontSize = 12.sp, color = Color(0xFF00668B), fontWeight = FontWeight.Medium)
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = when (item.status) {
                                        "APPROVED" -> Color(0xFFE8F5E9)
                                        "REJECTED" -> Color(0xFFFFEBEE)
                                        else -> Color(0xFFFFF3E0)
                                    }
                                ) {
                                    Text(
                                        when (item.status) {
                                            "APPROVED" -> "معتمد"
                                            "REJECTED" -> "مرفوض"
                                            else -> "قيد المراجعة"
                                        },
                                        color = when (item.status) {
                                            "APPROVED" -> Color(0xFF2E7D32)
                                            "REJECTED" -> Color(0xFFD32F2F)
                                            else -> Color(0xFFE65100)
                                        },
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Text("📍 الموقع: ${item.city} - ${item.neighborhood} | 📞 الهاتف: ${item.phone}", fontSize = 12.sp, color = Color(0xFF64748B))
                            Text("⏳ سنوات الخبرة: ${item.experienceYears} سنوات | 🆔 رقم الهوية: ${item.nationalId}", fontSize = 12.sp, color = Color(0xFF64748B))

                            if (item.notes.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("📝 سبب الرفض/ملاحظات: ${item.notes}", fontSize = 11.sp, color = Color(0xFFD32F2F))
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = Color(0xFFF1F5F9))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.US).format(Date(item.submissionDate)),
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (item.status != "APPROVED") {
                                        Button(
                                            onClick = {
                                                applicationsList = applicationsList.map {
                                                    if (it.id == item.id) it.copy(status = "APPROVED", notes = "") else it
                                                }
                                                adminViewModel.recordAuditLog("APPROVE_REGISTRATION", "قبول واعتماد طلب الانضمام ${item.id} (${item.applicantName})")
                                                Toast.makeText(context, "تم قبول واعتماد الطلب بنجاح وتفعيل الحساب", Toast.LENGTH_SHORT).show()
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("قبول وتفعيل", fontSize = 11.sp)
                                        }
                                    }

                                    if (item.status != "REJECTED") {
                                        OutlinedButton(
                                            onClick = {
                                                selectedAppForReview = item
                                                showRejectDialog = true
                                            },
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFD32F2F)),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("رفض", fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // نافذة سبب الرفض
    if (showRejectDialog && selectedAppForReview != null) {
        AlertDialog(
            onDismissRequest = { showRejectDialog = false },
            title = { Text("رفض طلب الانضمام", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("يرجى كتابة سبب رفض الطلب (${selectedAppForReview!!.applicantName}):", fontSize = 13.sp)
                    OutlinedTextField(
                        value = rejectReason,
                        onValueChange = { rejectReason = it },
                        placeholder = { Text("مثال: الوثائق غير واضحة، يلزم تقديم رخصة مهنية سارية") },
                        modifier = Modifier.fillMaxWidth().height(100.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val appId = selectedAppForReview!!.id
                        applicationsList = applicationsList.map {
                            if (it.id == appId) it.copy(status = "REJECTED", notes = rejectReason) else it
                        }
                        adminViewModel.recordAuditLog("REJECT_REGISTRATION", "رفض طلب الانضمام $appId بالسبب: $rejectReason")
                        showRejectDialog = false
                        rejectReason = ""
                        Toast.makeText(context, "تم رفض الطلب وإشعار المتقدم", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                ) {
                    Text("تأكيد الرفض")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRejectDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }
}
