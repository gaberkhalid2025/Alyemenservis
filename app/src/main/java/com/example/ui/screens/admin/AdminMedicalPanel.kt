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

/**
 * 🏥 AdminMedicalPanel
 * لوحة إدارة القطاع الطبي والعيادات والمستشفيات والصيدليات والمختبرات
 */
data class MedicalFacilityItem(
    val id: String,
    val name: String,
    val type: String, // HOSPITAL, CLINIC, PHARMACY, LAB
    val doctorOrManager: String,
    val phone: String,
    val city: String,
    val neighborhood: String,
    val emergencyAvailable: Boolean = false,
    val isVerified: Boolean = true,
    val rating: Float = 4.8f
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminMedicalPanel(
    onBack: () -> Unit = {},
    adminViewModel: AdminViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedTypeFilter by remember { mutableStateOf("ALL") }
    var showAddDialog by remember { mutableStateOf(false) }

    var facilityName by remember { mutableStateOf("") }
    var facilityType by remember { mutableStateOf("CLINIC") }
    var managerName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("صنعاء") }
    var neighborhood by remember { mutableStateOf("") }
    var hasEmergency by remember { mutableStateOf(false) }

    var facilitiesList by remember {
        mutableStateOf(
            listOf(
                MedicalFacilityItem(
                    id = "MED-01",
                    name = "مستشفى الأمل التخصصي",
                    type = "HOSPITAL",
                    doctorOrManager = "د. وليد الصبري",
                    phone = "01-445566",
                    city = "صنعاء",
                    neighborhood = "الستين الغربي",
                    emergencyAvailable = true,
                    isVerified = true,
                    rating = 4.9f
                ),
                MedicalFacilityItem(
                    id = "MED-02",
                    name = "عيادة رعاية الأسنان التخصصية",
                    type = "CLINIC",
                    doctorOrManager = "د. ريم المقالح",
                    phone = "770011223",
                    city = "عدن",
                    neighborhood = "خور مكسر",
                    emergencyAvailable = false,
                    isVerified = true,
                    rating = 4.7f
                ),
                MedicalFacilityItem(
                    id = "MED-03",
                    name = "صيدلية الحياة الكبرى",
                    type = "PHARMACY",
                    doctorOrManager = "د. صيدلي حسام القدسي",
                    phone = "733998877",
                    city = "تعز",
                    neighborhood = "شارع 26 سبتمبر",
                    emergencyAvailable = true,
                    isVerified = true,
                    rating = 4.8f
                ),
                MedicalFacilityItem(
                    id = "MED-04",
                    name = "مختبرات النخبة للتحاليل الطبية",
                    type = "LAB",
                    doctorOrManager = "أ.د. عصام النعمان",
                    phone = "711223344",
                    city = "إب",
                    neighborhood = "الدائري الغربي",
                    emergencyAvailable = false,
                    isVerified = true,
                    rating = 4.6f
                )
            )
        )
    }

    val filteredList = remember(facilitiesList, searchQuery, selectedTypeFilter) {
        facilitiesList.filter { item ->
            val matchesType = when (selectedTypeFilter) {
                "HOSPITAL" -> item.type == "HOSPITAL"
                "CLINIC" -> item.type == "CLINIC"
                "PHARMACY" -> item.type == "PHARMACY"
                "LAB" -> item.type == "LAB"
                else -> true
            }
            val matchesSearch = searchQuery.isBlank() ||
                    item.name.contains(searchQuery, ignoreCase = true) ||
                    item.doctorOrManager.contains(searchQuery, ignoreCase = true) ||
                    item.city.contains(searchQuery, ignoreCase = true) ||
                    item.phone.contains(searchQuery, ignoreCase = true)
            matchesType && matchesSearch
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("إدارة المنشآت والخدمات الطبية", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        Text("إجمالي المنشآت: ${facilitiesList.size}", fontSize = 11.sp, color = Color.Gray)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "إضافة منشأة", tint = Color(0xFF00668B))
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
                placeholder = { Text("بحث بالاسم، المسؤول، المدينة، أو الهاتف...", fontSize = 13.sp) },
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

            // تصفيات المنشآت
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val filters = listOf(
                    "ALL" to "الكل (${facilitiesList.size})",
                    "HOSPITAL" to "مستشفيات 🏥",
                    "CLINIC" to "عيادات ومراكز 🩺",
                    "PHARMACY" to "صيدليات 💊",
                    "LAB" to "مختبرات 🧪"
                )
                items(filters) { (key, label) ->
                    val isSelected = selectedTypeFilter == key
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedTypeFilter = key },
                        label = { Text(label, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF00668B),
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // قائمة المنشآت
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
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .background(
                                                when (item.type) {
                                                    "HOSPITAL" -> Color(0xFFFFEBEE)
                                                    "CLINIC" -> Color(0xFFE0F2FE)
                                                    "PHARMACY" -> Color(0xFFE8F5E9)
                                                    else -> Color(0xFFF3E5F5)
                                                },
                                                CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            when (item.type) {
                                                "HOSPITAL" -> Icons.Default.Favorite
                                                "CLINIC" -> Icons.Default.LocationOn
                                                "PHARMACY" -> Icons.Default.ShoppingCart
                                                else -> Icons.Default.Info
                                            },
                                            contentDescription = null,
                                            tint = when (item.type) {
                                                "HOSPITAL" -> Color(0xFFD32F2F)
                                                "CLINIC" -> Color(0xFF00668B)
                                                "PHARMACY" -> Color(0xFF2E7D32)
                                                else -> Color(0xFF7B1FA2)
                                            },
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }

                                    Column {
                                        Text(item.name, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF1E293B))
                                        Text("المسؤول: ${item.doctorOrManager}", fontSize = 12.sp, color = Color(0xFF64748B))
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    if (item.emergencyAvailable) {
                                        Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFFFFEBEE)) {
                                            Text("طوارئ 24h 🚨", color = Color(0xFFD32F2F), fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                                        }
                                    }
                                    Text("★ ${item.rating}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE65100))
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Text("📍 ${item.city} - ${item.neighborhood} | 📞 ${item.phone}", fontSize = 12.sp, color = Color(0xFF475569))

                            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = Color(0xFFF1F5F9))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("النوع: ${when(item.type) { "HOSPITAL" -> "مستشفى عام" "CLINIC" -> "عيادة متخصصة" "PHARMACY" -> "صيدلية" else -> "مختبر تحاليل" }}", fontSize = 11.sp, color = Color.Gray)

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    TextButton(onClick = {
                                        val newVer = !item.isVerified
                                        facilitiesList = facilitiesList.map { if (it.id == item.id) it.copy(isVerified = newVer) else it }
                                        adminViewModel.recordAuditLog("TOGGLE_MEDICAL_VERIFY", "تغيير حالة توثيق المنشأة ${item.name}")
                                        Toast.makeText(context, if (newVer) "تم توثيق المنشأة" else "تم إلغاء التوثيق", Toast.LENGTH_SHORT).show()
                                    }) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp), tint = if (item.isVerified) Color(0xFF2E7D32) else Color.Gray)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(if (item.isVerified) "موثقة ✓" else "غير موثقة", fontSize = 11.sp, color = if (item.isVerified) Color(0xFF2E7D32) else Color.Gray)
                                    }

                                    IconButton(
                                        onClick = {
                                            facilitiesList = facilitiesList.filterNot { it.id == item.id }
                                            adminViewModel.recordAuditLog("DELETE_MEDICAL_FACILITY", "حذف المنشأة الطبية ${item.name}")
                                            Toast.makeText(context, "تم حذف المنشأة بنجاح", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "حذف", tint = Color.LightGray, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // نافذة إضافة منشأة طبية جديدة
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("إضافة منشأة طبية جديدة", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = facilityName,
                        onValueChange = { facilityName = it },
                        label = { Text("اسم المنشأة الطبية") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = managerName,
                        onValueChange = { managerName = it },
                        label = { Text("اسم الطبيب المسؤول / المدير") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("رقم الهاتف / الطوارئ") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = city,
                            onValueChange = { city = it },
                            label = { Text("المحافظة") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = neighborhood,
                            onValueChange = { neighborhood = it },
                            label = { Text("الحي / الشارع") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("خدمة طوارئ 24 ساعة 🚨", fontSize = 13.sp)
                        Switch(checked = hasEmergency, onCheckedChange = { hasEmergency = it })
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (facilityName.isNotBlank() && phone.isNotBlank()) {
                            val newFacility = MedicalFacilityItem(
                                id = "MED-${System.currentTimeMillis() % 1000}",
                                name = facilityName,
                                type = facilityType,
                                doctorOrManager = managerName,
                                phone = phone,
                                city = city,
                                neighborhood = neighborhood,
                                emergencyAvailable = hasEmergency
                            )
                            facilitiesList = listOf(newFacility) + facilitiesList
                            adminViewModel.recordAuditLog("ADD_MEDICAL_FACILITY", "إضافة المنشأة الطبية $facilityName")
                            showAddDialog = false
                            facilityName = ""
                            managerName = ""
                            phone = ""
                            neighborhood = ""
                            Toast.makeText(context, "تمت إضافة المنشأة الطبية بنجاح", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "يرجى تعبئة الحقول الأساسية", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00668B))
                ) {
                    Text("إضافة المنشأة")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }
}
