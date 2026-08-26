package com.example.ui.screens.register

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
import com.example.ui.screens.register.forms.*
import com.example.utils.VisualThemePalette

/**
 * 📝 ProviderRegisterFormLayout - المنسق الموحد والمنظم لكافة أنماط التسجيل والانضمام
 * يستدعي النماذج المستقلة والنظيفة من مجلد forms/ حسب النمط المختار
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
    val snackbarHostState = remember { SnackbarHostState() }

    val categories by viewModel.categories.collectAsState()
    var activeType by remember(regType) { mutableStateOf(RegistrationType.fromId(regType)) }
    var isSubmitting by remember { mutableStateOf(false) }

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
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // شريط تبويب نوع التسجيل
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

            // عرض النموذج المخصص حسب النمط المختار
            AnimatedContent(
                targetState = activeType,
                label = "RegistrationFormSwitch"
            ) { targetType ->
                when (targetType) {
                    RegistrationType.CLIENT -> ClientRegistrationForm(
                        themeColors = themeColors,
                        isLoading = isSubmitting,
                        onSubmitClient = { user, pass ->
                            isSubmitting = true
                            viewModel.registerGuestUser(
                                context = context,
                                name = user.name,
                                phone = user.phone,
                                residence = "${user.city} - ${user.neighborhood}",
                                password = pass
                            )
                            isSubmitting = false
                        }
                    )

                    RegistrationType.PROVIDER -> ProviderRegistrationForm(
                        themeColors = themeColors,
                        isLoading = isSubmitting,
                        categories = categories.map { it.id to it.name },
                        onSubmitProvider = { entity, pass ->
                            isSubmitting = true
                            viewModel.submitJoinForm(
                                context = context,
                                name = entity.name,
                                phone = entity.phone,
                                catId = entity.categoryId,
                                area = entity.area,
                                neighborhood = entity.localNeighborhood,
                                photoPath = entity.selfiePhotoBase64,
                                idCardPath = entity.idPhotoBase64,
                                gpsCoords = "",
                                workPhotos = entity.workPhotosBase64,
                                customCategoryName = entity.profession,
                                password = pass,
                                productAttachmentsJson = entity.productAttachmentsJson
                            )
                            isSubmitting = false
                        }
                    )

                    RegistrationType.STORE -> StoreRegistrationForm(
                        themeColors = themeColors,
                        isLoading = isSubmitting,
                        onSubmitStore = { entity, pass ->
                            isSubmitting = true
                            viewModel.submitJoinForm(
                                context = context,
                                name = entity.name,
                                phone = entity.phone,
                                catId = "STORE",
                                area = entity.area,
                                neighborhood = entity.localNeighborhood,
                                photoPath = entity.selfiePhotoBase64,
                                idCardPath = entity.idPhotoBase64,
                                gpsCoords = "",
                                customCategoryName = entity.profession,
                                password = pass
                            )
                            isSubmitting = false
                        }
                    )

                    RegistrationType.RESTAURANT -> RestaurantRegistrationForm(
                        themeColors = themeColors,
                        isLoading = isSubmitting,
                        onSubmitRestaurant = { entity, pass ->
                            isSubmitting = true
                            viewModel.submitJoinForm(
                                context = context,
                                name = entity.name,
                                phone = entity.phone,
                                catId = "RESTAURANT",
                                area = entity.area,
                                neighborhood = entity.localNeighborhood,
                                photoPath = entity.selfiePhotoBase64,
                                idCardPath = entity.idPhotoBase64,
                                gpsCoords = "",
                                customCategoryName = entity.profession,
                                password = pass
                            )
                            isSubmitting = false
                        }
                    )

                    RegistrationType.MEDICAL -> MedicalRegistrationForm(
                        themeColors = themeColors,
                        isLoading = isSubmitting,
                        onSubmitMedical = { entity, pass ->
                            isSubmitting = true
                            viewModel.submitJoinForm(
                                context = context,
                                name = entity.name,
                                phone = entity.phone,
                                catId = "MEDICAL",
                                area = entity.area,
                                neighborhood = entity.localNeighborhood,
                                photoPath = entity.selfiePhotoBase64,
                                idCardPath = entity.idPhotoBase64,
                                gpsCoords = "",
                                customCategoryName = entity.profession,
                                password = pass
                            )
                            isSubmitting = false
                        }
                    )

                    RegistrationType.PROPERTY -> PropertyRegistrationForm(
                        themeColors = themeColors,
                        isLoading = isSubmitting,
                        onSubmitProperty = { entity, pass ->
                            isSubmitting = true
                            viewModel.submitJoinForm(
                                context = context,
                                name = entity.name,
                                phone = entity.phone,
                                catId = "PROPERTY",
                                area = entity.area,
                                neighborhood = entity.localNeighborhood,
                                photoPath = entity.selfiePhotoBase64,
                                idCardPath = entity.idPhotoBase64,
                                gpsCoords = "",
                                customCategoryName = entity.profession,
                                password = pass
                            )
                            isSubmitting = false
                        }
                    )

                    RegistrationType.JOB -> JobRegistrationForm(
                        themeColors = themeColors,
                        isLoading = isSubmitting,
                        onSubmitJob = { entity, pass ->
                            isSubmitting = true
                            viewModel.submitJoinForm(
                                context = context,
                                name = entity.name,
                                phone = entity.phone,
                                catId = "JOB",
                                area = entity.area,
                                neighborhood = entity.localNeighborhood,
                                photoPath = entity.selfiePhotoBase64,
                                idCardPath = entity.idPhotoBase64,
                                gpsCoords = "",
                                customCategoryName = entity.profession,
                                password = pass
                            )
                            isSubmitting = false
                        }
                    )
                }
            }
        }
    }
}
