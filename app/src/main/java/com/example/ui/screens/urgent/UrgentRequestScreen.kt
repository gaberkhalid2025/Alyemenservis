package com.example.ui.screens.urgent

import androidx.compose.animation.*
import com.example.viewmodels.AuthViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

import com.example.viewmodels.UrgentUiState
import com.example.viewmodels.InstantRequestViewModel
import kotlinx.coroutines.launch

import com.example.ui.screens.urgent.components.UrgentFormFields

/**
 * 🚨 UrgentRequestScreen
 * شاشة طلب خدمة عاجلة خلال 30 دقيقة مع مؤقت فوري وتنبيهات أولوية قصوى.
 * تستخدم النمط المعماري MVVM مع InstantRequestViewModel وعرض الملاحظات عبر Snackbar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UrgentRequestScreen(
    authViewModel: AuthViewModel = viewModel(),
    urgentViewModel: InstantRequestViewModel = viewModel(),
    onNavigateBack: () -> Unit = {},
    onNavigateToUrgentList: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val currentUserId by authViewModel.currentUserId.collectAsState()
    val uiState by urgentViewModel.uiState.collectAsState()

    var customerPhone by remember { mutableStateOf("") }
    var customerName by remember { mutableStateOf("") }
    var selectedDepartment by remember { mutableStateOf("خدمات وفنيين") }
    var selectedCategory by remember { mutableStateOf("سباكة طارئة") }
    var serviceTitle by remember { mutableStateOf("") }
    var serviceDetails by remember { mutableStateOf("") }
    var selectedCity by remember { mutableStateOf("صنعاء") }
    var selectedArea by remember { mutableStateOf("") }
    var pinCode by remember { mutableStateOf("") }
    var isPinVisible by remember { mutableStateOf(false) }

    var createdRequestCode by remember { mutableStateOf<String?>(null) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var expandedCategoryDropdown by remember { mutableStateOf(false) }

    val subCategories = remember(selectedDepartment) {
        UrgentConstants.getSubCategories(selectedDepartment)
    }

    LaunchedEffect(selectedDepartment) {
        selectedCategory = subCategories.firstOrNull() ?: ""
    }

    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is UrgentUiState.Error -> {
                scope.launch {
                    snackbarHostState.showSnackbar(state.message)
                }
            }
            else -> {}
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFD32F2F))
                        Text(
                            text = "طلب عاجل - 30 دقيقة",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color(0xFFD32F2F)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFFFEBEE),
                    titleContentColor = Color(0xFFB71C1C)
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // شريط تنبيه 30 دقيقة فوري
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                border = BorderStroke(1.5.dp, Color(0xFFE53935))
            ) {
                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFFD32F2F), modifier = Modifier.size(32.dp))
                    Column {
                        Text("خدمة الاستجابة السريعة (30 دقيقة)", fontWeight = FontWeight.Bold, color = Color(0xFFB71C1C), fontSize = 15.sp)
                        Text("يتم إرسال إشعار فوري عالي الأولوية للفنيين الأقرب لموقعك لاستلام العروض خلال 30 دقيقة فقط.", fontSize = 12.sp, color = Color(0xFFC62828))
                    }
                }
            }

            // بيانات التواصل والموقع
            Text("بيانات التواصل والموقع", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            UrgentFormFields(
                customerName = customerName,
                onCustomerNameChange = { customerName = it },
                customerPhone = customerPhone,
                onCustomerPhoneChange = { customerPhone = it },
                selectedCity = selectedCity,
                onCitySelected = { selectedCity = it },
                selectedArea = selectedArea,
                onAreaChange = { selectedArea = it }
            )

            HorizontalDivider()

            // القسم والتخصص
            Text("قسم الخدمة العاجلة", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                UrgentConstants.departments.forEach { dept ->
                    FilterChip(
                        selected = selectedDepartment == dept,
                        onClick = { selectedDepartment = dept },
                        label = { Text(dept, fontSize = 12.sp) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = selectedCategory,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("التخصص الطارئ*") },
                    trailingIcon = {
                        IconButton(onClick = { expandedCategoryDropdown = true }) {
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("urgent_category")
                )
                DropdownMenu(expanded = expandedCategoryDropdown, onDismissRequest = { expandedCategoryDropdown = false }) {
                    subCategories.forEach { cat ->
                        DropdownMenuItem(text = { Text(cat) }, onClick = { selectedCategory = cat; expandedCategoryDropdown = false })
                    }
                }
            }

            // تفاصيل المشكلة العاجلة
            OutlinedTextField(
                value = serviceTitle,
                onValueChange = { serviceTitle = it },
                label = { Text("عنوان الحالة الطارئة (مثال: عطل كهربائي مفاجئ)*") },
                modifier = Modifier.fillMaxWidth().testTag("urgent_title"),
                singleLine = true
            )

            OutlinedTextField(
                value = serviceDetails,
                onValueChange = { serviceDetails = it },
                label = { Text("وصف الحالة الطارئة والمطلوب بالتفصيل*") },
                modifier = Modifier.fillMaxWidth().height(100.dp).testTag("urgent_details"),
                maxLines = 3
            )

            // رمز PIN للحماية
            Text("رمز PIN للحماية (4 أرقام)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            OutlinedTextField(
                value = pinCode,
                onValueChange = { if (it.length <= 4 && it.all { char -> char.isDigit() }) pinCode = it },
                label = { Text("رمز PIN سري (4 أرقام)*") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                trailingIcon = {
                    IconButton(onClick = { isPinVisible = !isPinVisible }) {
                        Icon(imageVector = if (isPinVisible) Icons.Default.Check else Icons.Default.Lock, contentDescription = null)
                    }
                },
                visualTransformation = if (isPinVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                modifier = Modifier.fillMaxWidth().testTag("urgent_pin_code"),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(10.dp))

            // زر إرسال الطلب العاجل
            val isLoading = uiState is UrgentUiState.Loading
            Button(
                onClick = {
                    if (customerPhone.isBlank() || serviceTitle.isBlank() || serviceDetails.isBlank() || selectedArea.isBlank()) {
                        scope.launch { snackbarHostState.showSnackbar("يرجى تعبئة كافة الحقول الإجبارية (*)") }
                        return@Button
                    }
                    if (pinCode.length < 4) {
                        scope.launch { snackbarHostState.showSnackbar("يرجى كتابة رمز PIN مكون من 4 أرقام") }
                        return@Button
                    }

                    urgentViewModel.createUrgentRequest(
                        customerName = customerName,
                        customerPhone = customerPhone,
                        selectedCity = selectedCity,
                        selectedArea = selectedArea,
                        selectedDepartment = selectedDepartment,
                        selectedCategory = selectedCategory,
                        serviceTitle = serviceTitle,
                        serviceDetails = serviceDetails,
                        pinCode = pinCode,
                        currentUserId = currentUserId,
                        onSuccess = { code ->
                            createdRequestCode = code
                            showSuccessDialog = true
                        },
                        onError = { err ->
                            scope.launch { snackbarHostState.showSnackbar(err) }
                        }
                    )
                },
                modifier = Modifier.fillMaxWidth().height(52.dp).testTag("submit_urgent_request_btn"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Icon(Icons.Default.Warning, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("إرسال الطلب العاجل (مؤقت 30 دقيقة)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }

            OutlinedButton(
                onClick = onNavigateBack,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("إلغاء والعودة")
            }
        }
    }

    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = {},
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFFD32F2F))
                    Text("تم تعميم الطلب العاجل!", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("تم تعميم طلبك الطارئ على جميع الفنيين المتواجدين في منطقتك.")
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("كود الطلب العاجل", fontSize = 12.sp, color = Color(0xFFB71C1C))
                            Text(createdRequestCode ?: "", fontSize = 24.sp, fontWeight = FontWeight.Black, color = Color(0xFFD32F2F))
                            Text("المهلة المحددة: 30 دقيقة لاستقبال العروض", fontSize = 12.sp, color = Color(0xFFC62828))
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSuccessDialog = false
                        onNavigateToUrgentList()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                ) {
                    Text("متابعة الطلب العاجل الآن")
                }
            }
        )
    }
}
