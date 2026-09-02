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
import com.example.ui.viewmodels.AdminViewModel
import com.example.ui.viewmodels.PendingRequest
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

    val pendingRequestsState by adminViewModel.pendingRequests.collectAsState(initial = emptyList())
    val pendingRequests = pendingRequestsState

    LaunchedEffect(Unit) {
        adminViewModel.loadPendingRequests()
    }

    var selectedFilter by remember { mutableStateOf("الكل") }
    val filters = listOf("الكل", "معلق ⏳", "مرفوض ❌", "مقبول ✓")

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
            AdminFilterChips(
                categories = filters,
                selectedCategory = selectedFilter,
                onSelectCategory = { selectedFilter = it },
                themeColors = themeColors
            )

            if (pendingRequests.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("لا توجد طلبات انضمام معلقة حالياً 🎉", color = Color.Gray, fontSize = 13.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(pendingRequests, key = { it.id }) { req ->
                        AdminEntityCard(
                            title = req.name.ifBlank { "متقدم جديد" },
                            subtitle = "🛠️ ${req.section} • 📱 ${req.phone} • 📍 ${req.city}",
                            details = "📝 التفاصيل: ${req.details.ifBlank { "لا يوجد ملاحظات إضافية" }}",
                            statusText = "معلق ⏳",
                            statusColor = Color(0xFFF59E0B),
                            themeColors = themeColors,
                            actions = {
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
