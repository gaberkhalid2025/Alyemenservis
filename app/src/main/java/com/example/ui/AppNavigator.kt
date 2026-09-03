package com.example.ui

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.UnifiedBusinessAccount
import com.example.ui.screens.admin.AdminPanelLayout
import com.example.ui.screens.dashboard.*
import com.example.ui.screens.register.*
import com.example.utils.VisualThemePalette

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigator(
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    permissionLauncher: androidx.activity.result.ActivityResultLauncher<Array<String>>,
    locationPermissions: Array<String>
) {
    val context = LocalContext.current
    val currentScreen by viewModel.currentScreen.collectAsState()
    
    val currentUserName by viewModel.currentUserName.collectAsState()
    val currentUserPhone by viewModel.currentUserPhone.collectAsState()
    val currentUserResidence by viewModel.currentUserResidence.collectAsState()
    val currentUserId by viewModel.currentUserId.collectAsState()
    val bookings by viewModel.bookings.collectAsState()

    val sp = remember { context.getSharedPreferences("yemen_service_prefs", Context.MODE_PRIVATE) }
    var isLoggedIn by remember { mutableStateOf(sp.getBoolean("is_logged_in", false)) }
    var userType by remember { mutableStateOf(sp.getString("user_type", "CLIENT") ?: "CLIENT") }
    
    var homeClickCount by remember { mutableIntStateOf(0) }
    var showAdminLoginDialog by remember { mutableStateOf(false) }
    var adminEmailInput by remember { mutableStateOf("mah73646@gmail.com") }
    var adminPassInput by remember { mutableStateOf("Maher@@--@@736462##") }

    var showRestoreDialog by remember { mutableStateOf(false) }
    var restorePhone by remember { mutableStateOf("") }
    var restorePass by remember { mutableStateOf("") }
    var isRestoring by remember { mutableStateOf(false) }

    val dummyAccount = remember(currentUserId, currentUserName, currentUserPhone, currentUserResidence) {
        UnifiedBusinessAccount(
            id = currentUserId.ifEmpty { "user_1" },
            name = currentUserName.ifEmpty { "مستخدم المنصة" },
            phone = currentUserPhone.ifEmpty { "770000000" },
            neighborhood = currentUserResidence.ifEmpty { "صنعاء" }
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(themeColors.surface)) {
        when (currentScreen) {
            "REGISTER" -> {
                RegisterScreen(
                    viewModel = viewModel,
                    themeColors = themeColors,
                    onBackClick = { viewModel.navigateTo("USER_BROWSE") },
                    onOpenRestoreDialog = { showRestoreDialog = true }
                )
            }
            "JOIN_STATUS" -> {
                JoinRequestStatusScreen(
                    viewModel = viewModel,
                    themeColors = themeColors
                )
            }
            "ADMIN_PANEL" -> {
                AdminPanelLayout(
                    viewModel = viewModel,
                    themeColors = themeColors
                )
            }
            "CLIENT_DASHBOARD" -> {
                ClientPersonalAccountDashboard(
                    viewModel = viewModel,
                    themeColors = themeColors,
                    context = context,
                    currentUserName = currentUserName,
                    currentUserPhone = currentUserPhone,
                    currentUserResidence = currentUserResidence,
                    currentUserId = currentUserId,
                    bookings = bookings,
                    onShowRegistrationFormsAnyway = { viewModel.navigateTo("REGISTER") },
                    onNavigateToSupportChat = { viewModel.navigateTo("USER_BROWSE") }
                )
            }
            "TECHNICIAN_DASHBOARD" -> {
                TechnicianDashboard(
                    account = dummyAccount,
                    viewModel = viewModel,
                    themeColors = themeColors,
                    onBackClick = { viewModel.navigateTo("USER_BROWSE") }
                )
            }
            "STORE_DASHBOARD" -> {
                StoreDashboard(
                    account = dummyAccount,
                    viewModel = viewModel,
                    themeColors = themeColors,
                    onBackClick = { viewModel.navigateTo("USER_BROWSE") }
                )
            }
            "RESTAURANT_DASHBOARD" -> {
                RestaurantDashboard(
                    account = dummyAccount,
                    viewModel = viewModel,
                    themeColors = themeColors,
                    onBackClick = { viewModel.navigateTo("USER_BROWSE") }
                )
            }
            "MEDICAL_DASHBOARD" -> {
                MedicalDashboard(
                    account = dummyAccount,
                    viewModel = viewModel,
                    themeColors = themeColors,
                    onBackClick = { viewModel.navigateTo("USER_BROWSE") }
                )
            }
            "PROPERTY_DASHBOARD" -> {
                PropertyDashboard(
                    account = dummyAccount,
                    viewModel = viewModel,
                    themeColors = themeColors,
                    onBackClick = { viewModel.navigateTo("USER_BROWSE") }
                )
            }
            "JOB_DASHBOARD" -> {
                JobPosterDashboard(
                    account = dummyAccount,
                    viewModel = viewModel,
                    themeColors = themeColors,
                    onBackClick = { viewModel.navigateTo("USER_BROWSE") }
                )
            }
            else -> {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                Text(
                                    text = "🇾🇪 دليل خدمات اليمن الشامل",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            },
                            actions = {
                                IconButton(
                                    onClick = {
                                        homeClickCount++
                                        if (homeClickCount >= 5) {
                                            homeClickCount = 0
                                            showAdminLoginDialog = true
                                        } else {
                                            Toast.makeText(context, "الرئيسية (${5 - homeClickCount} نقرات متبقية للوحة الأدمن)", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.testTag("home_backdoor_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Home,
                                        contentDescription = "الرئيسية (لوحة الأدمن بالضغط 5 مرات)",
                                        tint = themeColors.accent
                                    )
                                }

                                IconButton(onClick = { showRestoreDialog = true }) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "استرجاع الحساب",
                                        tint = Color.White
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F172A))
                        )
                    },
                    containerColor = themeColors.surface
                ) { padding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Text(
                                    text = "🌟 أهلاً بك في دليل خدمات اليمن",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                                Text(
                                    text = "المنصة المتكاملة لطلب الخدمات، الفنيين، المتاجر، العقارات والوظائف في اليمن.",
                                    fontSize = 13.sp,
                                    color = Color.LightGray,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    lineHeight = 20.sp
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                Button(
                                    onClick = { viewModel.navigateTo("REGISTER") },
                                    colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                                    modifier = Modifier.fillMaxWidth().height(50.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = "📝 تسجيل حساب جديد أو انضمام",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black
                                    )
                                }

                                OutlinedButton(
                                    onClick = { showRestoreDialog = true },
                                    modifier = Modifier.fillMaxWidth().height(50.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, themeColors.accent)
                                ) {
                                    Text(
                                        text = "🔓 استرجاع حساب سابق",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showRestoreDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreDialog = false },
            containerColor = Color(0xFF1E293B),
            title = {
                Text("🔓 استرجاع الحساب", color = Color.White, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "أدخل رقم هاتفك اليمني (9 أرقام) وكلمة المرور الخاصة بك لاسترجاع حسابك وجلستك فوراً.",
                        fontSize = 12.sp,
                        color = Color.LightGray
                    )
                    OutlinedTextField(
                        value = restorePhone,
                        onValueChange = { restorePhone = it },
                        label = { Text("رقم الهاتف (9 أرقام)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = themeColors.accent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                    OutlinedTextField(
                        value = restorePass,
                        onValueChange = { restorePass = it },
                        label = { Text("كلمة المرور") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = themeColors.accent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val phoneClean = restorePhone.trim()
                        val passClean = restorePass.trim()
                        if (phoneClean.length == 9) {
                            isRestoring = true
                            try {
                                viewModel.restoreGuestUser(
                                    context = context,
                                    phone = phoneClean,
                                    password = passClean,
                                    onResult = { success, msg ->
                                        isRestoring = false
                                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                        if (success) {
                                            showRestoreDialog = false
                                            isLoggedIn = true
                                            val spUpdated = context.getSharedPreferences("yemen_service_prefs", Context.MODE_PRIVATE)
                                            userType = spUpdated.getString("user_type", "CLIENT") ?: "CLIENT"
                                        }
                                    }
                                )
                            } catch (e: Exception) {
                                isRestoring = false
                                Toast.makeText(context, "❌ حدث خطأ: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(context, "❌ يرجى إدخال رقم هاتف صحيح من 9 أرقام", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent)
                ) {
                    Text(if (isRestoring) "جاري الاسترجاع..." else "استرجاع الحساب", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreDialog = false }) {
                    Text("إلغاء", color = Color.White)
                }
            }
        )
    }

    if (showAdminLoginDialog) {
        AlertDialog(
            onDismissRequest = { showAdminLoginDialog = false },
            containerColor = Color(0xFF1E293B),
            title = {
                Text("🔐 دخول الأدمن (بوابة خلفية)", color = Color.White, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = adminEmailInput,
                        onValueChange = { adminEmailInput = it },
                        label = { Text("البريد الإلكتروني") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = themeColors.accent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                    OutlinedTextField(
                        value = adminPassInput,
                        onValueChange = { adminPassInput = it },
                        label = { Text("كلمة المرور") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = themeColors.accent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (adminEmailInput == "mah73646@gmail.com" && adminPassInput == "Maher@@--@@736462##") {
                            viewModel.authenticateAdmin("ADMIN")
                            showAdminLoginDialog = false
                            Toast.makeText(context, "✅ تم تسجيل دخول الأدمن بنجاح", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "❌ بيانات الدخول غير صحيحة", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent)
                ) {
                    Text("دخول الأدمن", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAdminLoginDialog = false }) {
                    Text("إلغاء", color = Color.White)
                }
            }
        )
    }
}
