package com.example.ui.screens.register

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
import com.example.ui.dialogs.RestoreAccountDialog
import com.example.ui.screens.register.forms.*
import com.example.ui.screens.register.status.JoinStatus
import com.example.ui.screens.register.status.JoinStatusUseCase
import com.example.utils.VisualThemePalette

/**
 * 📝 RegisterScreen - الشاشة المتكاملة لخيارات التسجيل والانضمام بـ "دليل خدمات اليمن"
 * - تعرض حقول التسجيل المكتملة لجميع الأقسام (فنيين، متاجر، مطاعم، مراكز، عقارات، وظائف، مستخدمين)
 * - تتحول لشاشة انتظار تفاعلية فور إرسال الطلب
 * - تتحول لشاشة الملف الشخصي ومساحة العمل فور موافقة الأدمن
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    onBackClick: () -> Unit = {},
    onSelectRegistrationType: (RegistrationType) -> Unit = {},
    onOpenRestoreDialog: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showRestoreDialog by remember { mutableStateOf(false) }
    var selectedType by remember { mutableStateOf<RegistrationType?>(null) }
    var forceShowForm by remember { mutableStateOf(false) }

    val joinPhone by viewModel.joinRequestPhone.collectAsState()
    val pendingProviders by viewModel.pendingProviders.collectAsState()
    val providers by viewModel.providers.collectAsState()
    val notifications by viewModel.notifications.collectAsState()
    val stores by viewModel.stores.collectAsState()
    val properties by viewModel.properties.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val jobs by viewModel.jobs.collectAsState()
    val registeredUsersList by viewModel.registeredUsersList.collectAsState()

    val useCase = remember { JoinStatusUseCase() }
    val currentStatus = remember(joinPhone, pendingProviders, providers, stores, properties, categories, notifications, jobs, registeredUsersList) {
        useCase.determineStatus(
            joinPhone = joinPhone,
            pendingProviders = pendingProviders,
            providers = providers,
            stores = stores,
            properties = properties,
            categories = categories,
            notifications = notifications,
            jobs = jobs,
            registeredUsersList = registeredUsersList
        )
    }

    // إذا كان للمستخدم طلب قيد الانتظار أو تم اعتماده، نعرض شاشة الانتظار أو الملف الشخصي المعتمد مباشرة
    if (currentStatus !is JoinStatus.NoRequest && !forceShowForm) {
        JoinRequestStatusScreen(
            viewModel = viewModel,
            themeColors = themeColors,
            modifier = modifier
        )
        return
    }

    // دوال إرسال النموذج وتوجيهه بحسب القسم
    val handleFormSubmit: (Map<String, Any>, RegistrationType) -> Unit = { data, type ->
        val phone = (data["phone"] as? String)?.trim() ?: ""
        val password = (data["password"] as? String) ?: ""
        val area = (data["city"] as? String) ?: "صنعاء"

        when (type) {
            RegistrationType.PROVIDER -> {
                val fullName = (data["fullName"] as? String) ?: "فني جديد"
                val craftType = (data["craftType"] as? String) ?: "صيانة عامة"
                viewModel.submitJoinForm(
                    context = context,
                    name = fullName,
                    phone = phone,
                    catId = craftType,
                    area = area,
                    neighborhood = (data["geographicalScope"] as? String) ?: "",
                    photoPath = "",
                    idCardPath = "",
                    gpsCoords = "",
                    customCategoryName = craftType,
                    password = password
                )
            }
            RegistrationType.STORE -> {
                val storeName = (data["storeName"] as? String) ?: "متجر جديد"
                val category = (data["storeCategory"] as? String) ?: "متجر تجاري"
                viewModel.submitJoinForm(
                    context = context,
                    name = storeName,
                    phone = phone,
                    catId = "STORE",
                    area = area,
                    neighborhood = "",
                    photoPath = "",
                    idCardPath = "",
                    gpsCoords = "",
                    customCategoryName = category,
                    password = password
                )
            }
            RegistrationType.RESTAURANT -> {
                val restaurantName = (data["restaurantName"] as? String) ?: "مطعم جديد"
                val cuisine = (data["cuisineType"] as? String) ?: "مطعم وكافيه"
                viewModel.submitJoinForm(
                    context = context,
                    name = restaurantName,
                    phone = phone,
                    catId = "RESTAURANT",
                    area = area,
                    neighborhood = "",
                    photoPath = "",
                    idCardPath = "",
                    gpsCoords = "",
                    customCategoryName = cuisine,
                    password = password
                )
            }
            RegistrationType.MEDICAL -> {
                val centerName = (data["medicalCenterName"] as? String) ?: (data["centerName"] as? String) ?: (data["clinicName"] as? String) ?: "مركز طبي"
                val specialty = (data["medicalSpecialties"] as? String) ?: "عيادات ومراكز طبية"
                viewModel.submitJoinForm(
                    context = context,
                    name = centerName,
                    phone = phone,
                    catId = "MEDICAL",
                    area = area,
                    neighborhood = "",
                    photoPath = "",
                    idCardPath = "",
                    gpsCoords = "",
                    customCategoryName = specialty,
                    password = password
                )
            }
            RegistrationType.PROPERTY -> {
                val propTitle = (data["officeName"] as? String) ?: (data["ownerName"] as? String) ?: "إعلان عقار"
                val propType = (data["propertyType"] as? String) ?: "عقار"
                viewModel.submitJoinForm(
                    context = context,
                    name = propTitle,
                    phone = phone,
                    catId = "PROPERTY",
                    area = area,
                    neighborhood = "",
                    photoPath = "",
                    idCardPath = "",
                    gpsCoords = "",
                    customCategoryName = propType,
                    password = password
                )
            }
            RegistrationType.JOB -> {
                val company = (data["companyName"] as? String) ?: "إعلان وظيفة"
                val title = (data["jobTitle"] as? String) ?: "وظيفة شاغرة"
                viewModel.submitJoinForm(
                    context = context,
                    name = company,
                    phone = phone,
                    catId = "JOB",
                    area = area,
                    neighborhood = "",
                    photoPath = "",
                    idCardPath = "",
                    gpsCoords = "",
                    customCategoryName = title,
                    password = password
                )
            }
            RegistrationType.CLIENT -> {
                val clientName = (data["fullName"] as? String) ?: "مستخدم جديد"
                val residence = (data["residence"] as? String) ?: area
                viewModel.registerClientUser(clientName, phone, residence, password)
                viewModel.submitJoinForm(
                    context = context,
                    name = clientName,
                    phone = phone,
                    catId = "CLIENT",
                    area = residence,
                    neighborhood = residence,
                    photoPath = "",
                    idCardPath = "",
                    gpsCoords = "",
                    customCategoryName = "حساب عميل",
                    password = password
                )
            }
        }

        // حفظ الهاتف وتحديث الواجهة لتتحول مباشرة إلى شاشة الانتظار
        val cleanPhone = phone.trim().replace(" ", "").replace("+", "")
        val sp = context.getSharedPreferences("yemen_service_prefs", android.content.Context.MODE_PRIVATE)
        sp.edit().putString("join_request_phone", cleanPhone).apply()

        forceShowForm = false
        selectedType = null
    }

    // واجهة التسجيل (اختيار النوع أو تعبئة الاستمارة)
    Scaffold(
        topBar = {
            if (selectedType == null) {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "📝 التسجيل والانضمام",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "منصتك الشاملة للخدمات والأنشطة في اليمن",
                                fontSize = 10.5.sp,
                                color = themeColors.accent
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                onBackClick()
                            }
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "رجوع",
                                tint = Color.White
                            )
                        }
                    },
                    actions = {
                        if (currentStatus !is JoinStatus.NoRequest) {
                            TextButton(onClick = { forceShowForm = false }) {
                                Text("حالة طلبي ⏳", color = themeColors.accent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F172A))
                )
            }
        },
        containerColor = Color(0xFF0F172A),
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (selectedType == null) {
                // 1. شاشة اختيار نوع الحساب من الـ 7 خيارات
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // 🌟 Simple Title Header
                    item {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(bottom = 4.dp)
                        ) {
                            Text(
                                text = "📝 منصة الانضمام والتسجيل",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "اختر القسم المناسب لك لتعبئة طلبك المباشر والبدء بالعمل:",
                                fontSize = 11.sp,
                                color = Color.LightGray
                            )
                        }
                    }

                    // 🎯 Section Header
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "📌 اختر نوع الحساب المناسب لك:",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = themeColors.accent
                            )
                            Text(
                                text = "7 خيارات معتمدة",
                                fontSize = 10.5.sp,
                                color = Color.Gray
                            )
                        }
                    }

                    // 📱 Grid of Registration Types (7 Options)
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            RegistrationType.values().forEach { regType ->
                                Card(
                                    onClick = {
                                        selectedType = regType
                                        onSelectRegistrationType(regType)
                                    },
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(46.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(themeColors.accent.copy(alpha = 0.15f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(regType.icon, fontSize = 22.sp)
                                        }

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = regType.title,
                                                fontSize = 13.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = regType.description,
                                                fontSize = 10.5.sp,
                                                color = Color.LightGray,
                                                lineHeight = 15.sp
                                            )
                                        }

                                        Button(
                                            onClick = {
                                                selectedType = regType
                                                onSelectRegistrationType(regType)
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = themeColors.accent),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Text(
                                                text = "تعبئة 👈",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.Black
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 🔓 Restore Account Link
                    item {
                        Card(
                            onClick = {
                                showRestoreDialog = true
                                onOpenRestoreDialog()
                            },
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                            border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("🔓", fontSize = 20.sp)
                                    Column {
                                        Text(
                                            text = "لديك حساب بالفعل في دليل الخدمات؟",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Text(
                                            text = "اضغط هنا لاسترجاع حسابك وحجوزاتك السابقة بالكامل",
                                            fontSize = 10.5.sp,
                                            color = themeColors.accent
                                        )
                                    }
                                }
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "استرجاع",
                                    tint = themeColors.accent
                                )
                            }
                        }
                    }

                    // 📜 Bottom Terms Footer
                    item {
                        Text(
                            text = "بالتسجيل بالمنصة، فإنك توافق على شروط الاستخدام وسياسة حماية البيانات بالجمهورية اليمنية.",
                            fontSize = 10.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp)
                        )
                    }
                }
            } else {
                // 2. شاشة استمارة التسجيل المحددة بحقولها الكاملة والواضحة
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    // شريط معلومات القسم المختار - مبسط جداً وموفر للمساحة
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            IconButton(
                                onClick = { selectedType = null },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "رجوع",
                                    tint = Color.White
                                )
                            }
                            Text(selectedType?.icon ?: "📋", fontSize = 18.sp)
                            Text(
                                text = "استمارة: ${selectedType?.title}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        TextButton(
                            onClick = { selectedType = null },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("تغيير 🔁", color = themeColors.accent, fontSize = 11.sp)
                        }
                    }

                    // عرض النموذج المناسب حسب نوع التسجيل المختار
                    Box(modifier = Modifier.weight(1f)) {
                        when (selectedType) {
                            RegistrationType.PROVIDER -> {
                                ProviderForm(
                                    themeColors = themeColors,
                                    onSubmit = { data -> handleFormSubmit(data, RegistrationType.PROVIDER) }
                                )
                            }
                            RegistrationType.STORE -> {
                                StoreForm(
                                    themeColors = themeColors,
                                    onSubmit = { data -> handleFormSubmit(data, RegistrationType.STORE) }
                                )
                            }
                            RegistrationType.RESTAURANT -> {
                                RestaurantForm(
                                    themeColors = themeColors,
                                    onSubmit = { data -> handleFormSubmit(data, RegistrationType.RESTAURANT) }
                                )
                            }
                            RegistrationType.MEDICAL -> {
                                MedicalForm(
                                    themeColors = themeColors,
                                    onSubmit = { data -> handleFormSubmit(data, RegistrationType.MEDICAL) }
                                )
                            }
                            RegistrationType.PROPERTY -> {
                                PropertyForm(
                                    themeColors = themeColors,
                                    onSubmit = { data -> handleFormSubmit(data, RegistrationType.PROPERTY) }
                                )
                            }
                            RegistrationType.JOB -> {
                                JobForm(
                                    themeColors = themeColors,
                                    onSubmit = { data -> handleFormSubmit(data, RegistrationType.JOB) }
                                )
                            }
                            RegistrationType.CLIENT, null -> {
                                UnifiedRegistrationForm(
                                    role = "CLIENT",
                                    themeColors = themeColors,
                                    onRegistrationSuccess = { map ->
                                        handleFormSubmit(map, RegistrationType.CLIENT)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showRestoreDialog) {
        RestoreAccountDialog(
            viewModel = viewModel,
            themeColors = themeColors,
            onDismiss = { showRestoreDialog = false }
        )
    }
}

/**
 * Backwards compatibility wrapper expected by main router
 */
@Composable
fun RegisterScreenLayout() {
    // Legacy placeholder wrapper
}
