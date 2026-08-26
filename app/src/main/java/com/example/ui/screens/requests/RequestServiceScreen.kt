package com.example.ui.screens.requests

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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.InstantRequestEntity
import com.example.ui.MainViewModel
import com.example.ui.components.AppSnackbarHost
import com.example.ui.components.SnackbarType
import com.example.ui.components.showCustomSnackbar
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.random.Random

/**
 * 📝 RequestServiceScreen
 * شاشة إنشاء طلب خدمة فوري بخطوات بسيطة وكود فريد وحماية برمز PIN
 * مرتبطة مع RequestsViewModel و AppSnackbar لإشعارات مخصصة وتصميم متناسق
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestServiceScreen(
    viewModel: MainViewModel,
    requestsViewModel: RequestsViewModel = viewModel(),
    onNavigateBack: () -> Unit = {},
    onNavigateToMyRequests: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val currentUserId by viewModel.currentUserId.collectAsState()
    val currentUserName by viewModel.currentUserName.collectAsState()
    val currentUserPhone by viewModel.currentUserPhone.collectAsState()

    var customerPhone by remember { mutableStateOf(currentUserPhone) }
    var customerName by remember { mutableStateOf(currentUserName) }
    var selectedDepartment by remember { mutableStateOf("خدمات وفنيين") }
    var selectedCategory by remember { mutableStateOf("سباكة") }
    var serviceTitle by remember { mutableStateOf("") }
    var serviceDetails by remember { mutableStateOf("") }
    var selectedCity by remember { mutableStateOf("صنعاء") }
    var selectedArea by remember { mutableStateOf("") }
    var urgencyTime by remember { mutableStateOf("خلال ساعة") }
    var pinCode by remember { mutableStateOf("") }
    var isPinVisible by remember { mutableStateOf(false) }

    var isSubmitting by remember { mutableStateOf(false) }
    var createdRequestCode by remember { mutableStateOf<String?>(null) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var expandedCategoryDropdown by remember { mutableStateOf(false) }
    var expandedCityDropdown by remember { mutableStateOf(false) }

    val departments = listOf("خدمات وفنيين", "مراكز ومتاجر", "مطاعم وكافيهات")
    val subCategories = when (selectedDepartment) {
        "خدمات وفنيين" -> listOf("سباكة", "كهرباء", "تكييف وتبريد", "صيانة سيارات", "أجهزة منزلية", "نجارة", "ألمنيوم وزجاج")
        "مراكز ومتاجر" -> listOf("قطع غيار", "أدوات ومعدات", "مواد بناء", "إلكترونيات", "أدوات صحية")
        else -> listOf("مطاعم يمنية", "وجبات سريعة", "مشويات", "كافيهات ومشروبات")
    }

    val urgencyOptions = listOf("فوراً (خلال 30 دقيقة)", "خلال ساعة", "خلال ساعتين", "اليوم مساءً", "غداً")
    val cities = listOf("صنعاء", "عدن", "تعز", "الحديدة", "إب", "حضرموت", "مأرب", "ذمار", "عمران")

    LaunchedEffect(selectedDepartment) {
        selectedCategory = subCategories.firstOrNull() ?: ""
    }

    Scaffold(
        snackbarHost = { AppSnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("اطلب خدمتك الآن", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
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
            // بطاقة تعليمات
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(
                        text = "اكتب طلبك وسيتم إرساله فوراً لجميع مقدمي الخدمات المتاحين في منطقتك لتستقبل عروض أسعار تنافسية!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            // 1. بيانات التواصل والموقع
            Text("1. بيانات التواصل والموقع", fontWeight = FontWeight.Bold, fontSize = 16.sp)

            OutlinedTextField(
                value = customerName,
                onValueChange = { customerName = it },
                label = { Text("اسم العميل") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                modifier = Modifier.fillMaxWidth().testTag("req_customer_name"),
                singleLine = true
            )

            OutlinedTextField(
                value = customerPhone,
                onValueChange = { customerPhone = it },
                label = { Text("رقم الهاتف (إجباري)*") },
                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth().testTag("req_customer_phone"),
                singleLine = true
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = selectedCity,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("المدينة*") },
                        trailingIcon = {
                            IconButton(onClick = { expandedCityDropdown = true }) {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                        },
                        modifier = Modifier.fillMaxWidth().testTag("req_city_dropdown")
                    )
                    DropdownMenu(
                        expanded = expandedCityDropdown,
                        onDismissRequest = { expandedCityDropdown = false }
                    ) {
                        cities.forEach { city ->
                            DropdownMenuItem(
                                text = { Text(city) },
                                onClick = {
                                    selectedCity = city
                                    expandedCityDropdown = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = selectedArea,
                    onValueChange = { selectedArea = it },
                    label = { Text("الحي / الشارع*") },
                    modifier = Modifier.weight(1f).testTag("req_area_input"),
                    singleLine = true
                )
            }

            HorizontalDivider()

            // 2. تصنيف وتفاصيل الخدمة
            Text("2. تفاصيل الخدمة المطلوبة", fontWeight = FontWeight.Bold, fontSize = 16.sp)

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                departments.forEach { dept ->
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
                    label = { Text("القسم الفرعي / التخصص*") },
                    trailingIcon = {
                        IconButton(onClick = { expandedCategoryDropdown = true }) {
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("req_category_dropdown")
                )
                DropdownMenu(
                    expanded = expandedCategoryDropdown,
                    onDismissRequest = { expandedCategoryDropdown = false }
                ) {
                    subCategories.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(cat) },
                            onClick = {
                                selectedCategory = cat
                                expandedCategoryDropdown = false
                            }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = serviceTitle,
                onValueChange = { serviceTitle = it },
                label = { Text("عنوان الطلب (مثال: صيانة تسريب مياه في المطبخ)*") },
                modifier = Modifier.fillMaxWidth().testTag("req_title_input"),
                singleLine = true
            )

            OutlinedTextField(
                value = serviceDetails,
                onValueChange = { serviceDetails = it },
                label = { Text("شرح المشكلة والمطلوب بالتفصيل*") },
                modifier = Modifier.fillMaxWidth().height(100.dp).testTag("req_details_input"),
                maxLines = 3
            )

            // 3. وقت الحضور ورمز الحماية
            Text("3. وقت الحضور ورمز الحماية", fontWeight = FontWeight.Bold, fontSize = 16.sp)

            Text("الوقت المناسب لحضور الفني:*", style = MaterialTheme.typography.bodyMedium)
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                urgencyOptions.forEach { opt ->
                    FilterChip(
                        selected = urgencyTime == opt,
                        onClick = { urgencyTime = opt },
                        label = { Text(opt, fontSize = 12.sp) }
                    )
                }
            }

            OutlinedTextField(
                value = pinCode,
                onValueChange = { if (it.length <= 4 && it.all { char -> char.isDigit() }) pinCode = it },
                label = { Text("رمز PIN سري (4 أرقام لحماية وإلغاء الطلب)*") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                trailingIcon = {
                    IconButton(onClick = { isPinVisible = !isPinVisible }) {
                        Icon(imageVector = if (isPinVisible) Icons.Default.Check else Icons.Default.Lock, contentDescription = null)
                    }
                },
                visualTransformation = if (isPinVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                modifier = Modifier.fillMaxWidth().testTag("req_pin_input"),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            // زر إرسال الطلب
            Button(
                onClick = {
                    if (customerPhone.isBlank() || serviceTitle.isBlank() || serviceDetails.isBlank() || selectedArea.isBlank()) {
                        scope.launch {
                            snackbarHostState.showCustomSnackbar(
                                message = "يرجى تعبئة كافة الحقول الإجبارية (*)",
                                type = SnackbarType.WARNING
                            )
                        }
                        return@Button
                    }
                    if (pinCode.length < 4) {
                        scope.launch {
                            snackbarHostState.showCustomSnackbar(
                                message = "يرجى إدخال رمز PIN مكون من 4 أرقام",
                                type = SnackbarType.WARNING
                            )
                        }
                        return@Button
                    }

                    isSubmitting = true
                    val uniqueCode = "REQ-${Random.nextInt(100000, 999999)}"
                    val reqId = UUID.randomUUID().toString()
                    val now = System.currentTimeMillis()
                    val request = InstantRequestEntity(
                        id = reqId,
                        requestCode = uniqueCode,
                        secretPin = pinCode,
                        cancellationPassword = pinCode,
                        userId = if (currentUserId.isNotBlank()) currentUserId else customerPhone,
                        userName = customerName.ifBlank { "عميل" },
                        userPhone = customerPhone,
                        userCity = selectedCity,
                        userNeighborhood = selectedArea,
                        categoryId = selectedDepartment,
                        categoryName = selectedCategory,
                        serviceTitle = serviceTitle,
                        description = serviceDetails,
                        status = "WAITING_FOR_OFFERS",
                        urgencyTime = urgencyTime,
                        createdAt = now,
                        expiresAt = now + 24 * 60 * 60 * 1000L
                    )

                    requestsViewModel.createRequest(
                        request = request,
                        onSuccess = {
                            isSubmitting = false
                            createdRequestCode = uniqueCode
                            showSuccessDialog = true
                        },
                        onError = { errMsg ->
                            isSubmitting = false
                            scope.launch {
                                snackbarHostState.showCustomSnackbar(
                                    message = "فشل إرسال الطلب: $errMsg",
                                    type = SnackbarType.ERROR
                                )
                            }
                        }
                    )
                },
                modifier = Modifier.fillMaxWidth().height(52.dp).testTag("submit_request_btn"),
                shape = RoundedCornerShape(12.dp),
                enabled = !isSubmitting
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Icon(Icons.Default.Send, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("نشر الطلب واستقبال العروض", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    }

    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = {},
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF2E7D32))
                    Text("تم نشر الطلب بنجاح!", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("تم تعميم طلبك على الفنيين المتاحين. ستتلقى عروض الأسعار قريباً.")
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("كود الطلب الخاص بك", fontSize = 12.sp)
                            Text(createdRequestCode ?: "", fontSize = 22.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                            Text("احتفظ برمز PIN لإلغاء أو تعديل الطلب.", fontSize = 11.sp, color = Color.DarkGray)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSuccessDialog = false
                        onNavigateToMyRequests()
                    }
                ) {
                    Text("متابعة الطلبات والعروض")
                }
            }
        )
    }
}
