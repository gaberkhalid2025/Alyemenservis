@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.ui.screens.register

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.ui.MainViewModel
import com.example.ui.screens.register.components.*
import com.example.util.FirebaseStorageUploader
import com.example.util.Validators
import com.example.utils.VisualThemePalette
import kotlinx.coroutines.launch

/**
 * 📝 ProviderRegisterFormLayout - النموذج الموحد والنظيف لكافة أنماط التسجيل والانضمام بـ "دليل خدمات اليمن"
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderRegisterFormLayout(
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    regType: String,
    sectionId: String = "",
    onRegTypeChange: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val currentUserName by viewModel.currentUserName.collectAsState()
    val currentUserPhone by viewModel.currentUserPhone.collectAsState()
    val currentUserResidence by viewModel.currentUserResidence.collectAsState()
    val categories by viewModel.categories.collectAsState()

    var activeType by remember(regType) { mutableStateOf(RegistrationType.fromId(regType)) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "نموذج تسجيل: ${activeType.title}",
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "دليل خدمات اليمن - استكمال البيانات المعيارية",
                            fontSize = 10.5.sp,
                            color = themeColors.accent
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.cancelOrResetJoinRequest(context) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "رجوع",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F172A))
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color(0xFF0F172A)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Category selector tabs
            ScrollableTabRow(
                selectedTabIndex = RegistrationType.values().indexOf(activeType),
                containerColor = Color(0xFF1E293B),
                contentColor = themeColors.accent,
                edgePadding = 8.dp
            ) {
                RegistrationType.values().forEach { item ->
                    Tab(
                        selected = activeType == item,
                        onClick = {
                            activeType = item
                            onRegTypeChange(item.id)
                        },
                        text = {
                            Text(
                                text = "${item.icon} ${item.title}",
                                fontSize = 11.5.sp,
                                fontWeight = if (activeType == item) FontWeight.Bold else FontWeight.Normal,
                                color = if (activeType == item) themeColors.accent else Color.Gray
                            )
                        }
                    )
                }
            }

            // Form content based on selected type
            when (activeType) {
                RegistrationType.CLIENT -> ClientForm(
                    viewModel = viewModel,
                    themeColors = themeColors,
                    snackbarHostState = snackbarHostState,
                    initialName = currentUserName,
                    initialPhone = currentUserPhone,
                    initialResidence = currentUserResidence
                )
                RegistrationType.PROVIDER -> ProviderForm(
                    viewModel = viewModel,
                    themeColors = themeColors,
                    categories = categories,
                    snackbarHostState = snackbarHostState,
                    initialName = currentUserName,
                    initialPhone = currentUserPhone,
                    initialResidence = currentUserResidence
                )
                RegistrationType.STORE -> StoreForm(
                    viewModel = viewModel,
                    themeColors = themeColors,
                    snackbarHostState = snackbarHostState,
                    initialName = currentUserName,
                    initialPhone = currentUserPhone
                )
                RegistrationType.RESTAURANT -> RestaurantForm(
                    viewModel = viewModel,
                    themeColors = themeColors,
                    snackbarHostState = snackbarHostState,
                    initialName = currentUserName,
                    initialPhone = currentUserPhone
                )
                RegistrationType.MEDICAL -> MedicalForm(
                    viewModel = viewModel,
                    themeColors = themeColors,
                    snackbarHostState = snackbarHostState,
                    initialName = currentUserName,
                    initialPhone = currentUserPhone
                )
                RegistrationType.PROPERTY -> PropertyForm(
                    viewModel = viewModel,
                    themeColors = themeColors,
                    snackbarHostState = snackbarHostState,
                    initialName = currentUserName,
                    initialPhone = currentUserPhone
                )
                RegistrationType.JOB -> JobForm(
                    viewModel = viewModel,
                    themeColors = themeColors,
                    snackbarHostState = snackbarHostState,
                    initialName = currentUserName,
                    initialPhone = currentUserPhone
                )
            }
        }
    }
}

// --------------------------------------------------------------------------------------------------
// 👤 1. Client Form
// --------------------------------------------------------------------------------------------------
@Composable
private fun ClientForm(
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    snackbarHostState: SnackbarHostState,
    initialName: String,
    initialPhone: String,
    initialResidence: String
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var name by remember { mutableStateOf(initialName) }
    var phone by remember { mutableStateOf(initialPhone) }
    var residence by remember { mutableStateOf(initialResidence) }
    var password by remember { mutableStateOf("") }
    var termsChecked by remember { mutableStateOf(false) }

    var isLoading by remember { mutableStateOf(false) }
    var phoneError by remember { mutableStateOf<String?>(null) }
    var nameError by remember { mutableStateOf<String?>(null) }

    RegistrationSection(
        title = "بيانات حساب العميل",
        subtitle = "إنشاء حساب للاستفادة من طلب الخدمات المباشرة باليمن",
        icon = "👤",
        themeColors = themeColors
    ) {
        RegistrationField(
            value = name,
            onValueChange = { name = it; nameError = null },
            label = "الاسم الكامل (إجباري) *",
            leadingIcon = Icons.Default.Person,
            errorMessage = nameError,
            themeColors = themeColors
        )

        RegistrationField(
            value = phone,
            onValueChange = { phone = it; phoneError = null },
            label = "رقم الهاتف اليمني (77x / 73x / 71x) *",
            placeholder = "771234567",
            leadingIcon = Icons.Default.Phone,
            errorMessage = phoneError,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            themeColors = themeColors
        )

        RegistrationField(
            value = residence,
            onValueChange = { residence = it },
            label = "المحافظة/المدينة والحي السكني *",
            leadingIcon = Icons.Default.Place,
            themeColors = themeColors
        )

        RegistrationField(
            value = password,
            onValueChange = { password = it },
            label = "كلمة المرور *",
            leadingIcon = Icons.Default.Lock,
            isPassword = true,
            themeColors = themeColors
        )

        RegistrationTermsCheckbox(
            checked = termsChecked,
            onCheckedChange = { termsChecked = it },
            themeColors = themeColors
        )

        RegistrationSubmitButton(
            text = "إنشاء حساب العميل الان 🚀",
            onClick = {
                val nameVal = Validators.validateName(name, "الاسم")
                if (!nameVal.isValid) {
                    nameError = nameVal.errorMessage
                    return@RegistrationSubmitButton
                }

                val phoneVal = Validators.validateYemenPhone(phone)
                if (!phoneVal.isValid) {
                    phoneError = phoneVal.errorMessage
                    return@RegistrationSubmitButton
                }

                if (!termsChecked) {
                    scope.launch { snackbarHostState.showSnackbar("يرجى الموافقة على شروط الاستخدام أولاً") }
                    return@RegistrationSubmitButton
                }

                isLoading = true
                val cleanPhone = if (phone.trim().length == 9) phone.trim() else "77${phone.trim()}"
                viewModel.registerGuestUser(context, name.trim(), cleanPhone, residence.trim(), password.trim())
                isLoading = false
                scope.launch { snackbarHostState.showSnackbar("🎉 تم تسجيل حسابك بنجاح!") }
            },
            isLoading = isLoading,
            enabled = termsChecked,
            themeColors = themeColors
        )
    }
}

// --------------------------------------------------------------------------------------------------
// 🔧 2. Provider / Technician Form
// --------------------------------------------------------------------------------------------------
@Composable
private fun ProviderForm(
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    categories: List<CategoryEntity>,
    snackbarHostState: SnackbarHostState,
    initialName: String,
    initialPhone: String,
    initialResidence: String
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var name by remember { mutableStateOf(initialName) }
    var phone by remember { mutableStateOf(initialPhone) }
    var password by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(categories.firstOrNull()?.id ?: "") }
    var area by remember { mutableStateOf(initialResidence.ifEmpty { "صنعاء" }) }
    var neighborhood by remember { mutableStateOf("") }
    var photosUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var termsChecked by remember { mutableStateOf(false) }

    var isLoading by remember { mutableStateOf(false) }
    var isUploadingPhotos by remember { mutableStateOf(false) }
    var uploadProgress by remember { mutableStateOf(0f) }
    var phoneError by remember { mutableStateOf<String?>(null) }
    var nameError by remember { mutableStateOf<String?>(null) }

    val categoryOptions = remember(categories) {
        categories.map { CategoryOption(it.id, it.name) }
    }

    RegistrationSection(
        title = "تقديم طلب انضمام كفني / مهني معتمد",
        subtitle = "أدخل بياناتك الفنية والمستندات لاستقبال طلبات العمل فوراً",
        icon = "🔧",
        themeColors = themeColors
    ) {
        RegistrationField(
            value = name,
            onValueChange = { name = it; nameError = null },
            label = "الاسم الثلاثي للفني *",
            leadingIcon = Icons.Default.Person,
            errorMessage = nameError,
            themeColors = themeColors
        )

        RegistrationField(
            value = phone,
            onValueChange = { phone = it; phoneError = null },
            label = "رقم هاتف التواصل اليمني *",
            placeholder = "771234567",
            leadingIcon = Icons.Default.Phone,
            errorMessage = phoneError,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            themeColors = themeColors
        )

        RegistrationCategoryChips(
            title = "اختر التخصص والمجال الفني الرئيسي:",
            categories = categoryOptions,
            selectedId = selectedCategory,
            onCategorySelected = { selectedCategory = it },
            themeColors = themeColors
        )

        RegistrationField(
            value = area,
            onValueChange = { area = it },
            label = "المحافظة/المدينة الرئيسي *",
            leadingIcon = Icons.Default.Place,
            themeColors = themeColors
        )

        RegistrationField(
            value = neighborhood,
            onValueChange = { neighborhood = it },
            label = "الحي والشارع المفضل للعمل *",
            leadingIcon = Icons.Default.LocationOn,
            themeColors = themeColors
        )

        RegistrationField(
            value = password,
            onValueChange = { password = it },
            label = "كلمة المرور الخاصة بك *",
            leadingIcon = Icons.Default.Lock,
            isPassword = true,
            themeColors = themeColors
        )

        RegistrationImagePicker(
            title = "صور الهوية الشخصية وسوابق الأعمال (سيلفي / هوية / عمل)",
            subtitle = "أضف صورك لرفع التوثيق وسرعة الاعتماد الإداري",
            imagesUris = photosUris,
            onImagesSelected = { photosUris = it },
            onImageRemoved = { idx -> photosUris = photosUris.filterIndexed { index, _ -> index != idx } },
            isUploading = isUploadingPhotos,
            uploadProgress = uploadProgress,
            themeColors = themeColors
        )

        RegistrationTermsCheckbox(
            checked = termsChecked,
            onCheckedChange = { termsChecked = it },
            themeColors = themeColors
        )

        RegistrationSubmitButton(
            text = "إرسال طلب الانضمام كفني 🛠️",
            onClick = {
                val nameVal = Validators.validateName(name, "الاسم")
                if (!nameVal.isValid) {
                    nameError = nameVal.errorMessage
                    return@RegistrationSubmitButton
                }

                val phoneVal = Validators.validateYemenPhone(phone)
                if (!phoneVal.isValid) {
                    phoneError = phoneVal.errorMessage
                    return@RegistrationSubmitButton
                }

                if (selectedCategory.isEmpty()) {
                    scope.launch { snackbarHostState.showSnackbar("يرجى اختيار القسم والتخصص الفني") }
                    return@RegistrationSubmitButton
                }

                isLoading = true
                isUploadingPhotos = photosUris.isNotEmpty()

                val cleanPhone = if (phone.trim().length == 9) phone.trim() else "77${phone.trim()}"

                scope.launch {
                    val uploadedUrls = mutableListOf<String>()
                    photosUris.forEachIndexed { idx, uri ->
                        uploadProgress = (idx + 1).toFloat() / photosUris.size
                        val res = FirebaseStorageUploader.uploadImageUri(
                            context,
                            uri,
                            FirebaseStorageUploader.getProviderWorkPhotoPath(cleanPhone, idx)
                        )
                        res.getOrNull()?.let { uploadedUrls.add(it) }
                    }

                    val selfie = uploadedUrls.getOrNull(0) ?: ""
                    val idCard = uploadedUrls.getOrNull(1) ?: ""

                    viewModel.submitJoinForm(
                        context = context,
                        name = name.trim(),
                        phone = cleanPhone,
                        catId = selectedCategory,
                        area = area.trim(),
                        neighborhood = neighborhood.trim(),
                        photoPath = selfie,
                        idCardPath = idCard,
                        gpsCoords = "15.3694,44.1910",
                        workPhotos = uploadedUrls,
                        password = password.trim()
                    )

                    isLoading = false
                    isUploadingPhotos = false
                    snackbarHostState.showSnackbar("⏳ تم إرسال طلب انضمامك بنجاح! وهو قيد المراجعة الإدارية.")
                    viewModel.setJoinRequestPhone(context, cleanPhone)
                }
            },
            isLoading = isLoading,
            enabled = termsChecked,
            themeColors = themeColors
        )
    }
}

// --------------------------------------------------------------------------------------------------
// 🏪 3. Store Form
// --------------------------------------------------------------------------------------------------
@Composable
private fun StoreForm(
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    snackbarHostState: SnackbarHostState,
    initialName: String,
    initialPhone: String
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var storeName by remember { mutableStateOf("") }
    var ownerName by remember { mutableStateOf(initialName) }
    var phone by remember { mutableStateOf(initialPhone) }
    var city by remember { mutableStateOf("صنعاء") }
    var password by remember { mutableStateOf("") }
    var photosUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var termsChecked by remember { mutableStateOf(false) }

    var isLoading by remember { mutableStateOf(false) }

    RegistrationSection(
        title = "تسجيل متجر / معرض تجاري",
        subtitle = "إدراج محلك التجاري لعرض المنتجات والوصول إلى آلاف الزبائن",
        icon = "🏪",
        themeColors = themeColors
    ) {
        RegistrationField(
            value = storeName,
            onValueChange = { storeName = it },
            label = "اسم المتجر / المعرض *",
            leadingIcon = Icons.Default.ShoppingCart,
            themeColors = themeColors
        )

        RegistrationField(
            value = ownerName,
            onValueChange = { ownerName = it },
            label = "اسم صاحب المتجر / المسؤول *",
            leadingIcon = Icons.Default.Person,
            themeColors = themeColors
        )

        RegistrationField(
            value = phone,
            onValueChange = { phone = it },
            label = "رقم هاتف المتجر اليمني *",
            placeholder = "771234567",
            leadingIcon = Icons.Default.Phone,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            themeColors = themeColors
        )

        RegistrationField(
            value = city,
            onValueChange = { city = it },
            label = "المحافظة والمنطقة *",
            leadingIcon = Icons.Default.Place,
            themeColors = themeColors
        )

        RegistrationField(
            value = password,
            onValueChange = { password = it },
            label = "كلمة المرور لإدارة المتجر *",
            leadingIcon = Icons.Default.Lock,
            isPassword = true,
            themeColors = themeColors
        )

        RegistrationImagePicker(
            title = "صور المتجر والمعرض (واجهة المحل / المنتجات / الترخيص)",
            imagesUris = photosUris,
            onImagesSelected = { photosUris = it },
            onImageRemoved = { idx -> photosUris = photosUris.filterIndexed { index, _ -> index != idx } },
            themeColors = themeColors
        )

        RegistrationTermsCheckbox(
            checked = termsChecked,
            onCheckedChange = { termsChecked = it },
            themeColors = themeColors
        )

        RegistrationSubmitButton(
            text = "تسجيل المتجر وتفعيل الحساب 🏪",
            onClick = {
                val phoneVal = Validators.validateYemenPhone(phone)
                if (!phoneVal.isValid) {
                    scope.launch { snackbarHostState.showSnackbar(phoneVal.errorMessage ?: "خطأ برقم الهاتف") }
                    return@RegistrationSubmitButton
                }

                isLoading = true
                val cleanPhone = if (phone.trim().length == 9) phone.trim() else "77${phone.trim()}"

                scope.launch {
                    val uploadedUrls = mutableListOf<String>()
                    photosUris.forEachIndexed { idx, uri ->
                        val res = FirebaseStorageUploader.uploadImageUri(
                            context,
                            uri,
                            FirebaseStorageUploader.getStorePhotoPath(cleanPhone, idx)
                        )
                        res.getOrNull()?.let { uploadedUrls.add(it) }
                    }

                    val storeEntity = StoreEntity(
                        id = "store_$cleanPhone",
                        ownerId = cleanPhone,
                        name = storeName.ifBlank { "متجر $ownerName" },
                        ownerName = ownerName,
                        phone = cleanPhone,
                        cityId = city,
                        sectionId = "stores",
                        coverImage = uploadedUrls.getOrNull(0) ?: "",
                        logoImage = uploadedUrls.getOrNull(1) ?: "",
                        images = uploadedUrls,
                        isActive = false
                    )

                    viewModel.saveStore(storeEntity)
                    isLoading = false
                    snackbarHostState.showSnackbar("⏳ تم تسجيل طلب المتجر بنجاح وهو قيد الاعتماد!")
                    viewModel.setJoinRequestPhone(context, cleanPhone)
                }
            },
            isLoading = isLoading,
            enabled = termsChecked,
            themeColors = themeColors
        )
    }
}

// --------------------------------------------------------------------------------------------------
// 🍔 4. Restaurant Form
// --------------------------------------------------------------------------------------------------
@Composable
private fun RestaurantForm(
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    snackbarHostState: SnackbarHostState,
    initialName: String,
    initialPhone: String
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var restName by remember { mutableStateOf("") }
    var ownerName by remember { mutableStateOf(initialName) }
    var phone by remember { mutableStateOf(initialPhone) }
    var city by remember { mutableStateOf("صنعاء") }
    var password by remember { mutableStateOf("") }
    var photosUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var termsChecked by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    RegistrationSection(
        title = "تسجيل مطعم / كافيه",
        subtitle = "إضافة مطعمك أو الكافيه الخاص بك واستقبال طلبات القائمة المباشرة",
        icon = "🍔",
        themeColors = themeColors
    ) {
        RegistrationField(
            value = restName,
            onValueChange = { restName = it },
            label = "اسم المطعم / الكافيه *",
            leadingIcon = Icons.Default.ShoppingCart,
            themeColors = themeColors
        )

        RegistrationField(
            value = ownerName,
            onValueChange = { ownerName = it },
            label = "اسم المالك / المدير *",
            leadingIcon = Icons.Default.Person,
            themeColors = themeColors
        )

        RegistrationField(
            value = phone,
            onValueChange = { phone = it },
            label = "رقم هاتف الطلبات اليمني *",
            placeholder = "771234567",
            leadingIcon = Icons.Default.Phone,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            themeColors = themeColors
        )

        RegistrationField(
            value = city,
            onValueChange = { city = it },
            label = "المحافظة والحي *",
            leadingIcon = Icons.Default.Place,
            themeColors = themeColors
        )

        RegistrationField(
            value = password,
            onValueChange = { password = it },
            label = "كلمة المرور *",
            leadingIcon = Icons.Default.Lock,
            isPassword = true,
            themeColors = themeColors
        )

        RegistrationImagePicker(
            title = "صور القائمة (المنيو) والشعار والواجهة",
            imagesUris = photosUris,
            onImagesSelected = { photosUris = it },
            onImageRemoved = { idx -> photosUris = photosUris.filterIndexed { index, _ -> index != idx } },
            themeColors = themeColors
        )

        RegistrationTermsCheckbox(
            checked = termsChecked,
            onCheckedChange = { termsChecked = it },
            themeColors = themeColors
        )

        RegistrationSubmitButton(
            text = "تسجيل المطعم الان 🍔",
            onClick = {
                val phoneVal = Validators.validateYemenPhone(phone)
                if (!phoneVal.isValid) {
                    scope.launch { snackbarHostState.showSnackbar(phoneVal.errorMessage ?: "خطأ برقم الهاتف") }
                    return@RegistrationSubmitButton
                }

                isLoading = true
                val cleanPhone = if (phone.trim().length == 9) phone.trim() else "77${phone.trim()}"

                scope.launch {
                    val uploadedUrls = mutableListOf<String>()
                    photosUris.forEachIndexed { idx, uri ->
                        val res = FirebaseStorageUploader.uploadImageUri(
                            context,
                            uri,
                            FirebaseStorageUploader.getStorePhotoPath(cleanPhone, idx)
                        )
                        res.getOrNull()?.let { uploadedUrls.add(it) }
                    }

                    val storeEntity = StoreEntity(
                        id = "rest_$cleanPhone",
                        ownerId = cleanPhone,
                        name = restName.ifBlank { "مطعم $ownerName" },
                        ownerName = ownerName,
                        phone = cleanPhone,
                        cityId = city,
                        sectionId = "restaurants",
                        coverImage = uploadedUrls.getOrNull(0) ?: "",
                        logoImage = uploadedUrls.getOrNull(1) ?: "",
                        images = uploadedUrls,
                        isActive = false
                    )

                    viewModel.saveStore(storeEntity)
                    isLoading = false
                    snackbarHostState.showSnackbar("⏳ تم تسجيل المطعم بنجاح وهو قيد المراجعة الإدارية!")
                    viewModel.setJoinRequestPhone(context, cleanPhone)
                }
            },
            isLoading = isLoading,
            enabled = termsChecked,
            themeColors = themeColors
        )
    }
}

// --------------------------------------------------------------------------------------------------
// 🏥 5. Medical Form
// --------------------------------------------------------------------------------------------------
@Composable
private fun MedicalForm(
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    snackbarHostState: SnackbarHostState,
    initialName: String,
    initialPhone: String
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var clinicName by remember { mutableStateOf("") }
    var doctorName by remember { mutableStateOf(initialName) }
    var phone by remember { mutableStateOf(initialPhone) }
    var city by remember { mutableStateOf("صنعاء") }
    var password by remember { mutableStateOf("") }
    var photosUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var termsChecked by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    RegistrationSection(
        title = "تسجيل مركز طبي / عيادة / صيدلية",
        subtitle = "توثيق المركز الطبي أو العيادة وتسهيل حجز المواعيد للمرضى",
        icon = "🏥",
        themeColors = themeColors
    ) {
        RegistrationField(
            value = clinicName,
            onValueChange = { clinicName = it },
            label = "اسم العيادة / المركز الطبي / الصيدلية *",
            leadingIcon = Icons.Default.Info,
            themeColors = themeColors
        )

        RegistrationField(
            value = doctorName,
            onValueChange = { doctorName = it },
            label = "اسم الطبيب / المدير المسؤول *",
            leadingIcon = Icons.Default.Person,
            themeColors = themeColors
        )

        RegistrationField(
            value = phone,
            onValueChange = { phone = it },
            label = "رقم هاتف التواصل للعيادة *",
            placeholder = "771234567",
            leadingIcon = Icons.Default.Phone,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            themeColors = themeColors
        )

        RegistrationField(
            value = city,
            onValueChange = { city = it },
            label = "المحافظة والمنطقة *",
            leadingIcon = Icons.Default.Place,
            themeColors = themeColors
        )

        RegistrationField(
            value = password,
            onValueChange = { password = it },
            label = "كلمة المرور *",
            leadingIcon = Icons.Default.Lock,
            isPassword = true,
            themeColors = themeColors
        )

        RegistrationImagePicker(
            title = "صور ترخيص مزاولة المهنة والعيادة والخدمات",
            imagesUris = photosUris,
            onImagesSelected = { photosUris = it },
            onImageRemoved = { idx -> photosUris = photosUris.filterIndexed { index, _ -> index != idx } },
            themeColors = themeColors
        )

        RegistrationTermsCheckbox(
            checked = termsChecked,
            onCheckedChange = { termsChecked = it },
            themeColors = themeColors
        )

        RegistrationSubmitButton(
            text = "تسجيل العيادة والمركز الطبي 🏥",
            onClick = {
                val phoneVal = Validators.validateYemenPhone(phone)
                if (!phoneVal.isValid) {
                    scope.launch { snackbarHostState.showSnackbar(phoneVal.errorMessage ?: "خطأ برقم الهاتف") }
                    return@RegistrationSubmitButton
                }

                isLoading = true
                val cleanPhone = if (phone.trim().length == 9) phone.trim() else "77${phone.trim()}"

                scope.launch {
                    val uploadedUrls = mutableListOf<String>()
                    photosUris.forEachIndexed { idx, uri ->
                        val res = FirebaseStorageUploader.uploadImageUri(
                            context,
                            uri,
                            FirebaseStorageUploader.getStorePhotoPath(cleanPhone, idx)
                        )
                        res.getOrNull()?.let { uploadedUrls.add(it) }
                    }

                    val storeEntity = StoreEntity(
                        id = "med_$cleanPhone",
                        ownerId = cleanPhone,
                        name = clinicName.ifBlank { "عيادة د. $doctorName" },
                        ownerName = doctorName,
                        phone = cleanPhone,
                        cityId = city,
                        sectionId = "medical",
                        coverImage = uploadedUrls.getOrNull(0) ?: "",
                        logoImage = uploadedUrls.getOrNull(1) ?: "",
                        images = uploadedUrls,
                        isActive = false
                    )

                    viewModel.saveStore(storeEntity)
                    isLoading = false
                    snackbarHostState.showSnackbar("⏳ تم تسجيل المركز الطبي بنجاح وهو قيد التوثيق الإداري!")
                    viewModel.setJoinRequestPhone(context, cleanPhone)
                }
            },
            isLoading = isLoading,
            enabled = termsChecked,
            themeColors = themeColors
        )
    }
}

// --------------------------------------------------------------------------------------------------
// 🏠 6. Property Form
// --------------------------------------------------------------------------------------------------
@Composable
private fun PropertyForm(
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    snackbarHostState: SnackbarHostState,
    initialName: String,
    initialPhone: String
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var propTitle by remember { mutableStateOf("") }
    var ownerName by remember { mutableStateOf(initialName) }
    var phone by remember { mutableStateOf(initialPhone) }
    var price by remember { mutableStateOf("") }
    var area by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("صنعاء") }
    var typeSelection by remember { mutableStateOf("شقة سكنية") }
    var transactionType by remember { mutableStateOf("إيجار") }
    var photosUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var termsChecked by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    RegistrationSection(
        title = "إدراج عقار للبيع أو الإيجار",
        subtitle = "عرض الشقق والمنازل والأراضي والمحلات للتواصل المباشر مع الباحثين",
        icon = "🏠",
        themeColors = themeColors
    ) {
        RegistrationField(
            value = propTitle,
            onValueChange = { propTitle = it },
            label = "عنوان العقار (مثال: شقة فاخرة للإيجار بحي الخمسين) *",
            leadingIcon = Icons.Default.Home,
            themeColors = themeColors
        )

        RegistrationField(
            value = ownerName,
            onValueChange = { ownerName = it },
            label = "اسم المالك / المكتب العقاري *",
            leadingIcon = Icons.Default.Person,
            themeColors = themeColors
        )

        RegistrationField(
            value = phone,
            onValueChange = { phone = it },
            label = "رقم هاتف التواصل اليمني *",
            placeholder = "771234567",
            leadingIcon = Icons.Default.Phone,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            themeColors = themeColors
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RegistrationField(
                value = price,
                onValueChange = { price = it },
                label = "السعر الإجمالي / الشهري *",
                leadingIcon = Icons.Default.Check,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                themeColors = themeColors,
                modifier = Modifier.weight(1f)
            )
            RegistrationField(
                value = area,
                onValueChange = { area = it },
                label = "المساحة (متر مربع) *",
                leadingIcon = Icons.Default.Place,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                themeColors = themeColors,
                modifier = Modifier.weight(1f)
            )
        }

        RegistrationField(
            value = city,
            onValueChange = { city = it },
            label = "المحافظة والمنطقة والحي *",
            leadingIcon = Icons.Default.Place,
            themeColors = themeColors
        )

        RegistrationImagePicker(
            title = "صور العقار (الغرف / الواجهة / التصميم)",
            imagesUris = photosUris,
            onImagesSelected = { photosUris = it },
            onImageRemoved = { idx -> photosUris = photosUris.filterIndexed { index, _ -> index != idx } },
            themeColors = themeColors
        )

        RegistrationTermsCheckbox(
            checked = termsChecked,
            onCheckedChange = { termsChecked = it },
            themeColors = themeColors
        )

        RegistrationSubmitButton(
            text = "إدراج العقار بالدليل 🏡",
            onClick = {
                val phoneVal = Validators.validateYemenPhone(phone)
                if (!phoneVal.isValid) {
                    scope.launch { snackbarHostState.showSnackbar(phoneVal.errorMessage ?: "خطأ برقم الهاتف") }
                    return@RegistrationSubmitButton
                }

                isLoading = true
                val cleanPhone = if (phone.trim().length == 9) phone.trim() else "77${phone.trim()}"

                scope.launch {
                    val uploadedUrls = mutableListOf<String>()
                    photosUris.forEachIndexed { idx, uri ->
                        val res = FirebaseStorageUploader.uploadImageUri(
                            context,
                            uri,
                            FirebaseStorageUploader.getPropertyPhotoPath(cleanPhone, idx)
                        )
                        res.getOrNull()?.let { uploadedUrls.add(it) }
                    }

                    val propEntity = PropertyEntity(
                        id = "prop_$cleanPhone",
                        ownerId = cleanPhone,
                        title = propTitle.ifBlank { "عقار - $typeSelection" },
                        price = price.toDoubleOrNull() ?: 0.0,
                        cityId = city,
                        type = transactionType,
                        propertyType = typeSelection,
                        phone = cleanPhone,
                        images = uploadedUrls,
                        isActive = false
                    )

                    viewModel.saveProperty(propEntity)
                    isLoading = false
                    snackbarHostState.showSnackbar("⏳ تم إدراج العقار بنجاح وهو قيد الاعتماد الظاهر!")
                    viewModel.setJoinRequestPhone(context, cleanPhone)
                }
            },
            isLoading = isLoading,
            enabled = termsChecked,
            themeColors = themeColors
        )
    }
}

// --------------------------------------------------------------------------------------------------
// 💼 7. Job Form
// --------------------------------------------------------------------------------------------------
@Composable
private fun JobForm(
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    snackbarHostState: SnackbarHostState,
    initialName: String,
    initialPhone: String
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var jobTitle by remember { mutableStateOf("") }
    var companyName by remember { mutableStateOf("") }
    var managerName by remember { mutableStateOf(initialName) }
    var phone by remember { mutableStateOf(initialPhone) }
    var city by remember { mutableStateOf("صنعاء") }
    var salary by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var termsChecked by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    RegistrationSection(
        title = "نشر إعلان وظيفة / شاغر شغلي",
        subtitle = "الإعلان عن الشواغر الوظيفية بالمنشأة واستقبال طلبات التوظيف المباشرة",
        icon = "💼",
        themeColors = themeColors
    ) {
        RegistrationField(
            value = jobTitle,
            onValueChange = { jobTitle = it },
            label = "المسمى الوظيفي المطلوب (مثال: محاسب قانوني) *",
            leadingIcon = Icons.Default.Edit,
            themeColors = themeColors
        )

        RegistrationField(
            value = companyName,
            onValueChange = { companyName = it },
            label = "اسم الشركة / المنشأة أو المحل *",
            leadingIcon = Icons.Default.Home,
            themeColors = themeColors
        )

        RegistrationField(
            value = managerName,
            onValueChange = { managerName = it },
            label = "اسم مسؤول التوظيف *",
            leadingIcon = Icons.Default.Person,
            themeColors = themeColors
        )

        RegistrationField(
            value = phone,
            onValueChange = { phone = it },
            label = "رقم هاتف استقبال طلبات التوظيف *",
            placeholder = "771234567",
            leadingIcon = Icons.Default.Phone,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            themeColors = themeColors
        )

        RegistrationField(
            value = city,
            onValueChange = { city = it },
            label = "موقع العمل (المحافظة والحي) *",
            leadingIcon = Icons.Default.Place,
            themeColors = themeColors
        )

        RegistrationField(
            value = salary,
            onValueChange = { salary = it },
            label = "الراتب المتوقع / الشروط المادية *",
            leadingIcon = Icons.Default.Check,
            themeColors = themeColors
        )

        RegistrationField(
            value = description,
            onValueChange = { description = it },
            label = "وصف الوصف الوظيفي والمؤهلات المطلوبة *",
            leadingIcon = Icons.Default.Info,
            singleLine = false,
            minLines = 3,
            maxLines = 5,
            themeColors = themeColors
        )

        RegistrationTermsCheckbox(
            checked = termsChecked,
            onCheckedChange = { termsChecked = it },
            themeColors = themeColors
        )

        RegistrationSubmitButton(
            text = "نشر إعلان الوظيفة الآن 💼",
            onClick = {
                val phoneVal = Validators.validateYemenPhone(phone)
                if (!phoneVal.isValid) {
                    scope.launch { snackbarHostState.showSnackbar(phoneVal.errorMessage ?: "خطأ برقم الهاتف") }
                    return@RegistrationSubmitButton
                }

                isLoading = true
                val cleanPhone = if (phone.trim().length == 9) phone.trim() else "77${phone.trim()}"

                scope.launch {
                    val jobEntity = JobEntity(
                        id = "job_$cleanPhone",
                        title = jobTitle.ifBlank { "شاغر وظيفي - $companyName" },
                        companyName = companyName,
                        managerName = managerName,
                        phone = cleanPhone,
                        cityId = city,
                        salary = salary,
                        description = description
                    )

                    viewModel.saveJob(jobEntity)
                    isLoading = false
                    snackbarHostState.showSnackbar("🎉 تم نشر إعلان الوظيفة بنجاح بالدليل!")
                    viewModel.setJoinRequestPhone(context, cleanPhone)
                }
            },
            isLoading = isLoading,
            enabled = termsChecked,
            themeColors = themeColors
        )
    }
}
