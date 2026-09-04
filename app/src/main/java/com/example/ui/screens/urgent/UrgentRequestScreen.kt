package com.example.ui.screens.urgent

import androidx.compose.animation.*
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
import com.example.ui.MainViewModel
import com.example.ui.viewmodels.InstantRequestViewModel
import com.example.ui.viewmodels.InstantUiState
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
    viewModel: MainViewModel,
    instantViewModel: InstantRequestViewModel = viewModel(),
    onNavigateBack: () -> Unit = {},
    onNavigateToUrgentList: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val currentUserId by viewModel.currentUserId.collectAsState()
    val uiState by instantViewModel.uiState.collectAsState()

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
            is InstantUiState.Error -> {
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
                        Text("طلب صيانة طارئة (30 دقيقة)", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // كارد تنبيه الطوارئ
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFD32F2F))
                    Text(
                        "🚨 سيتم إشعار جميع الفنيين المتاحين فوراً في نطاق منطقتك وتقديم عروض خلال 30 دقيقة كحد أقصى.",
                        fontSize = 13.sp,
                        color = Color(0xFFB71C1C),
                        fontWeight = FontWeight.Medium
                    )
                }
            }

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

            OutlinedTextField(
                value = serviceTitle,
                onValueChange = { serviceTitle = it },
                label = { Text("عنوان الخدمة الطارئة *") },
                placeholder = { Text("مثال: تسرب مياه طارئ في المطبخ") },
                modifier = Modifier.fillMaxWidth().testTag("urgent_service_title"),
                singleLine = true
            )

            OutlinedTextField(
                value = serviceDetails,
                onValueChange = { serviceDetails = it },
                label = { Text("تفاصيل المشكلة والخدمة المطلوب تنفيذها *") },
                placeholder = { Text("اكتب وصفاً توضيحياً للمشكلة لتمكين الفنيين من تقييم العمل وتقديم العرض المناسب...") },
                modifier = Modifier.fillMaxWidth().height(100.dp).testTag("urgent_service_details"),
                maxLines = 4
            )

            // رمز PIN للأمان والإلغاء
            OutlinedTextField(
                value = pinCode,
                onValueChange = { if (it.length <= 6) pinCode = it },
                label = { Text("رمز PIN سري خاص بك للتحكم بالطلب *") },
                placeholder = { Text("مثال: 1234 (لحفظ أمان طلبك وإلغائه)") },
                modifier = Modifier.fillMaxWidth().testTag("urgent_pin_input"),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                visualTransformation = if (isPinVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { isPinVisible = !isPinVisible }) {
                        Icon(
                            imageVector = if (isPinVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = "تبديل الرؤية"
                        )
                    }
                },
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            // زر إرسال الطلب العاجل
            val isLoading = uiState is InstantUiState.Loading
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

                    instantViewModel.createInstantRequest(
                        userId = currentUserId,
                        userName = customerName,
                        userPhone = customerPhone,
                        userCity = selectedCity,
                        userNeighborhood = selectedArea,
                        categoryId = selectedDepartment,
                        categoryName = selectedCategory,
                        serviceTitle = serviceTitle,
                        description = serviceDetails,
                        customPin = pinCode,
                        onResult = { success, msg, _ ->
                            if (success) {
                                createdRequestCode = "URG-${(1000..9999).random()}"
                                showSuccessDialog = true
                            } else {
                                scope.launch { snackbarHostState.showSnackbar(msg) }
                            }
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
                    Text("تم تعميم طلبك على الفنيين المتاحين فوراً.")
                    Text("رمز الطلب: ${createdRequestCode ?: "URG-XXXX"}", fontWeight = FontWeight.Bold, color = Color(0xFFD32F2F))
                    Text("⏳ ستبدأ العروض بالظهور خلال 30 دقيقة عبر قائمة الطلبات العاجلة.", fontSize = 13.sp)
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
                    Text("متابعة العروض الآن")
                }
            }
        )
    }
}
