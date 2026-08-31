package com.example.ui.screens.admin
import com.example.ui.MainViewModel

import androidx.compose.foundation.BorderStroke
import com.example.viewmodels.StoreViewModel
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
import com.example.data.StoreEntity

import com.example.ui.screens.admin.components.AdminEntityCard
import com.example.ui.screens.admin.components.AdminFilterChips
import com.example.utils.VisualThemePalette
import kotlinx.coroutines.launch

/**
 * 🏥 Admin Panel: Medical & Pharmacies Management (إدارة القطاع الطبي والصيدليات)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminMedicalPanel(
    onBack: () -> Unit = {},
    viewModel: MainViewModel = viewModel(),
    themeColors: VisualThemePalette,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val allStores by viewModel.stores.collectAsState()

    val medicalStores = remember(allStores) {
        allStores.filter { it.sectionId == "medical" || it.categoryId == "medical" || it.providerType == "medical" }
    }

    var selectedFilter by remember { mutableStateOf("الكل") }
    val filters = listOf("الكل", "صيدليات", "عيادات ومراكز", "محظور")

    var showAddDialog by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var cityId by remember { mutableStateOf("صنعاء") }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("🏥 إدارة المنشآت والصيدليات الطبية", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "إضافة منشأة", tint = themeColors.accent)
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

            if (medicalStores.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🏥 لا توجد منشآت طبية حالياً", color = Color.Gray, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { showAddDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent)
                        ) {
                            Text("إضافة منشأة جديدة", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(medicalStores, key = { it.id }) { store ->
                        AdminEntityCard(
                            title = store.name,
                            subtitle = "📱 ${store.phone} • 📍 ${store.cityId} - ${store.localNeighborhood}",
                            details = "ترخيص طبي: ${store.medicalLicenseNo.ifBlank { "غير متوفر" }}",
                            statusText = if (store.isBlocked) "محظور" else "نشط",
                            statusColor = if (store.isBlocked) Color(0xFFEF5350) else Color(0xFF10B981),
                            isBlocked = store.isBlocked,
                            themeColors = themeColors,
                            actions = {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            viewModel.toggleStoreBlocked(store.id, !store.isBlocked)
                                            scope.launch { snackbarHostState.showSnackbar(if (store.isBlocked) "تم إلغاء حظر المنشأة" else "تم حظر المنشأة") }
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, if (store.isBlocked) Color(0xFF10B981) else Color(0xFFEF5350)),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(if (store.isBlocked) "فك الحظر" else "حظر 🚫", fontSize = 10.5.sp, color = if (store.isBlocked) Color(0xFF10B981) else Color(0xFFEF5350))
                                    }

                                    IconButton(
                                        onClick = {
                                            viewModel.deleteStore(store.id)
                                            scope.launch { snackbarHostState.showSnackbar("🗑️ تم حذف المنشأة الطبية") }
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

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            containerColor = Color(0xFF1E293B),
            title = { Text("🏥 إضافة منشأة/صيدلية جديدة", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("اسم المنشأة/الصيدلية", color = Color.Gray) },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("رقم الهاتف التواصل", color = Color.Gray) },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            viewModel.addNewStore(
                                name = name,
                                phone = phone,
                                cityId = cityId,
                                localNeighborhood = "المركز",
                                categoryId = "medical",
                                coverImage = "",
                                workingHours = "24/7"
                            )
                            showAddDialog = false
                            name = ""; phone = ""
                            scope.launch { snackbarHostState.showSnackbar("✅ تم إضافة المنشأة بنجاح") }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent)
                ) {
                    Text("حفظ البيانات", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("إلغاء", color = Color.Gray)
                }
            }
        )
    }
}
