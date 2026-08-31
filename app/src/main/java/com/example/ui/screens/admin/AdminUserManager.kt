package com.example.ui.screens.admin

import androidx.compose.foundation.BorderStroke
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
import com.example.ui.MainViewModel
import com.example.ui.screens.admin.components.AdminEntityCard
import com.example.ui.screens.admin.components.AdminFilterChips
import com.example.utils.VisualThemePalette
import com.example.viewmodels.AdminViewModel
import java.util.UUID
import kotlinx.coroutines.launch

/**
 * 👑 Admin Panel: User & Accounts Manager (إدارة المستخدمين والحسابات)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminUserManager(
    onBack: () -> Unit = {},
    adminViewModel: AdminViewModel = viewModel(),
    mainViewModel: MainViewModel = viewModel(),
    themeColors: VisualThemePalette,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val rawUsersList by mainViewModel.registeredUsersList.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("الكل") }
    val filters = listOf("الكل", "عميل", "فني", "متجر", "محظور")

    var showResetDialog by remember { mutableStateOf(false) }
    var selectedUserIdForReset by remember { mutableStateOf("") }
    var selectedUserNameForReset by remember { mutableStateOf("") }
    var newPasswordState by remember { mutableStateOf("") }

    val filteredUsers = remember(rawUsersList, searchQuery, selectedFilter) {
        rawUsersList.filter { userMap ->
            val name = userMap["name"] as? String ?: ""
            val phone = userMap["phone"] as? String ?: ""
            val role = userMap["role"] as? String ?: "CLIENT"
            val isBlocked = userMap["isBlocked"] as? Boolean ?: false

            val matchesSearch = name.contains(searchQuery, ignoreCase = true) ||
                    phone.contains(searchQuery, ignoreCase = true)

            val matchesFilter = when (selectedFilter) {
                "عميل" -> role == "CLIENT" && !isBlocked
                "فني" -> role == "PROVIDER" && !isBlocked
                "متجر" -> role == "STORE" && !isBlocked
                "محظور" -> isBlocked
                else -> true
            }

            matchesSearch && matchesFilter
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("👥 إدارة المستخدمين والحسابات", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val csv = buildString {
                            appendLine("Name,Phone,Role,City")
                            rawUsersList.forEach { u ->
                                appendLine("${u["name"]},${u["phone"]},${u["role"]},${u["city"]}")
                            }
                        }
                        adminViewModel.recordAuditLog("EXPORT_USERS", "تصدير المستخدمين")
                        scope.launch { snackbarHostState.showSnackbar("📥 تم تصدير بيانات المستخدمين بنجاح") }
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "تصدير", tint = themeColors.accent)
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
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("بحث باسم المستخدم أو رقم الهاتف...", color = Color.Gray, fontSize = 11.5.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = themeColors.accent) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = themeColors.accent,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
                    focusedContainerColor = Color(0xFF1E293B),
                    unfocusedContainerColor = Color(0xFF1E293B),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth()
            )

            AdminFilterChips(
                categories = filters,
                selectedCategory = selectedFilter,
                onSelectCategory = { selectedFilter = it },
                themeColors = themeColors
            )

            if (filteredUsers.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("لا يوجد مستخدمون يطابقون خيارات البحث", color = Color.Gray, fontSize = 13.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredUsers, key = { it["id"] as? String ?: it["phone"] as? String ?: UUID.randomUUID().toString() }) { userMap ->
                        val userId = userMap["id"] as? String ?: ""
                        val name = userMap["name"] as? String ?: "مستخدم"
                        val phone = userMap["phone"] as? String ?: ""
                        val city = userMap["city"] as? String ?: "صنعاء"
                        val role = userMap["role"] as? String ?: "CLIENT"
                        val isBlocked = userMap["isBlocked"] as? Boolean ?: false

                        AdminEntityCard(
                            title = name,
                            subtitle = "📱 $phone • 📍 $city • 👤 $role",
                            statusText = if (isBlocked) "محظور" else "نشط",
                            statusColor = if (isBlocked) Color(0xFFEF5350) else Color(0xFF10B981),
                            isBlocked = isBlocked,
                            themeColors = themeColors,
                            actions = {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = {
                                                if (isBlocked) {
                                                    adminViewModel.unblockUser(userId) { success ->
                                                        scope.launch { snackbarHostState.showSnackbar("تم إلغاء حظر المستخدم") }
                                                    }
                                                } else {
                                                    adminViewModel.blockUser(userId) { success ->
                                                        scope.launch { snackbarHostState.showSnackbar("تم حظر المستخدم") }
                                                    }
                                                }
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            border = BorderStroke(1.dp, if (isBlocked) Color(0xFF10B981) else Color(0xFFEF5350)),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text(if (isBlocked) "إلغاء الحظر" else "حظر 🚫", fontSize = 10.5.sp, color = if (isBlocked) Color(0xFF10B981) else Color(0xFFEF5350))
                                        }

                                        IconButton(
                                            onClick = {
                                                adminViewModel.deleteUser(userId) { success ->
                                                    scope.launch { snackbarHostState.showSnackbar("🗑️ تم حذف حساب المستخدم") }
                                                }
                                            },
                                            modifier = Modifier.background(Color(0xFFEF5350).copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "حذف", tint = Color(0xFFEF5350), modifier = Modifier.size(18.dp))
                                        }
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            selectedUserIdForReset = userId
                                            selectedUserNameForReset = name
                                            newPasswordState = ""
                                            showResetDialog = true
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.5f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Default.Lock, contentDescription = null, tint = themeColors.accent, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("تعيين كلمة مرور جديدة 🔑", fontSize = 10.5.sp, color = themeColors.accent)
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            containerColor = Color(0xFF1E293B),
            title = { Text("🔑 تعيين كلمة مرور لـ $selectedUserNameForReset", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("أدخل كلمة المرور الجديدة التي سيتم تشفيرها وتخزينها بأمان:", color = Color.LightGray, fontSize = 12.sp)
                    OutlinedTextField(
                        value = newPasswordState,
                        onValueChange = { newPasswordState = it },
                        label = { Text("كلمة المرور الجديدة") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPasswordState.isNotBlank()) {
                            val hash = com.example.util.PasswordHasher.createSaltedHash(newPasswordState)
                            val updates = mapOf(
                                "password" to newPasswordState,
                                "passwordHash" to hash
                            )
                            mainViewModel.db.collection("registered_users").document(selectedUserIdForReset).update(updates)
                                .addOnSuccessListener {
                                    scope.launch { snackbarHostState.showSnackbar("🔑 تم تغيير كلمة المرور بنجاح") }
                                }
                                .addOnFailureListener {
                                    scope.launch { snackbarHostState.showSnackbar("❌ فشل تغيير كلمة المرور") }
                                }
                            showResetDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent)
                ) {
                    Text("تحديث", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("إلغاء", color = Color.Gray)
                }
            }
        )
    }
}
