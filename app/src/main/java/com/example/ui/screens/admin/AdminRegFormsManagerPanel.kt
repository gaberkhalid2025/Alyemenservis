package com.example.ui.screens.admin

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
import com.example.viewmodels.AdminViewModel
import com.example.viewmodels.PendingRequest
import kotlinx.coroutines.launch

/**
 * 📝 Admin Panel: Registration Requests & Applications Manager (إدارة طلبات الانضمام)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminRegFormsManagerPanel(
    onBack: () -> Unit = {},
    adminViewModel: AdminViewModel = viewModel(),
    themeColors: VisualThemePalette,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val pendingRequestsState by adminViewModel.pendingRequests.collectAsState()

    LaunchedEffect(Unit) {
        adminViewModel.loadPendingRequests()
    }

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("الكل") }
    val filters = listOf("الكل", "معلق ⏳", "مرفوض ❌", "مقبول ✓")

    val filteredRequests = remember(pendingRequestsState, selectedFilter, searchQuery) {
        pendingRequestsState.filter { req ->
            val matchesFilter = when (selectedFilter) {
                "معلق ⏳" -> req.status == "PENDING"
                "مرفوض ❌" -> req.status == "REJECTED"
                "مقبول ✓" -> req.status == "APPROVED"
                else -> true
            }
            val matchesSearch = searchQuery.isBlank() ||
                    req.name.contains(searchQuery, ignoreCase = true) ||
                    req.phone.contains(searchQuery) ||
                    req.section.contains(searchQuery, ignoreCase = true)

            matchesFilter && matchesSearch
        }.sortedByDescending { it.createdAt }
    }

    var rejectTarget by remember { mutableStateOf<PendingRequest?>(null) }
    var rejectReason by remember { mutableStateOf("") }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("📝 طلبات واستمارات الانضمام", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { adminViewModel.loadPendingRequests() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "تحديث", tint = themeColors.accent)
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
                placeholder = { Text("بحث باسم المتقدم أو رقم الهاتف...", color = Color.Gray, fontSize = 13.sp) },
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

            if (filteredRequests.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("لا توجد طلبات تطابق معايير البحث والفلترة 🎉", color = Color.Gray, fontSize = 13.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredRequests, key = { it.id }) { req ->
                        AdminEntityCard(
                            title = req.name.ifBlank { "متقدم جديد" },
                            subtitle = "🛠️ ${req.section} • 📱 ${req.phone} • 📍 ${req.city}",
                            details = "📝 التفاصيل: ${req.details.ifBlank { "لا يوجد ملاحظات إضافية" }}",
                            statusText = if (req.status == "APPROVED") "مقبول ✓" else if (req.status == "REJECTED") "مرفوض ❌" else "معلق ⏳",
                            statusColor = if (req.status == "APPROVED") Color(0xFF10B981) else if (req.status == "REJECTED") Color(0xFFEF5350) else Color(0xFFF59E0B),
                            themeColors = themeColors,
                            actions = {
                                if (req.status == "PENDING") {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = {
                                                adminViewModel.approveProviderRequest(req.id) { success ->
                                                    scope.launch {
                                                        snackbarHostState.showSnackbar(if (success) "✅ تم قبول الطلب واعتماد الحساب" else "❌ فشل قبول الطلب")
                                                    }
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("قبول وتفعيل ✓", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                        }

                                        Button(
                                            onClick = { rejectTarget = req },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF5350)),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("رفض الطلب ❌", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
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

    if (rejectTarget != null) {
        AlertDialog(
            onDismissRequest = { rejectTarget = null },
            containerColor = Color(0xFF1E293B),
            title = { Text("رفض طلب الانضمام", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("سبب رفض طلب ${rejectTarget?.name}:", color = Color.Gray, fontSize = 12.sp)
                    OutlinedTextField(
                        value = rejectReason,
                        onValueChange = { rejectReason = it },
                        placeholder = { Text("اكتب سبب الرفض هنا...", color = Color.DarkGray) },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val req = rejectTarget
                        if (req != null) {
                            adminViewModel.rejectProviderRequest(req.id, rejectReason.ifBlank { "عدم استيفاء الشروط" }) { success ->
                                rejectTarget = null
                                rejectReason = ""
                                scope.launch {
                                    snackbarHostState.showSnackbar(if (success) "تم رفض الطلب وإبلاغ المتقدم" else "فشل رفض الطلب")
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF5350))
                ) {
                    Text("تأكيد الرفض", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { rejectTarget = null }) { Text("إلغاء", color = Color.Gray) }
            }
        )
    }
}
