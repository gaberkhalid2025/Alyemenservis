package com.example.ui.screens.register.forms

import android.net.Uri
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import com.example.data.CategoryEntity
import com.example.ui.MainViewModel
import com.example.ui.screens.register.components.*
import com.example.util.FirebaseStorageUploader
import com.example.util.Validators
import com.example.utils.VisualThemePalette
import kotlinx.coroutines.launch

/**
 * 🔧 ProviderForm - نموذج تقديم طلب انضمام كفني / مهني معتمد
 */
@Composable
fun ProviderForm(
    viewModel: MainViewModel,
    formViewModel: RegistrationFormViewModel,
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

    val isSubmitting by formViewModel.isSubmitting.collectAsState()
    val formState by formViewModel.formState.collectAsState()
    var isUploadingPhotos by remember { mutableStateOf(false) }
    var uploadProgress by remember { mutableStateOf(0f) }
    var phoneError by remember { mutableStateOf<String?>(null) }
    var nameError by remember { mutableStateOf<String?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            formViewModel.resetState()
        }
    }

    LaunchedEffect(formState) {
        if (formState is FormUiState.Success) {
            password = ""
            isUploadingPhotos = false
        }
    }

    val onSubmit: () -> Unit = {
        val nameVal = Validators.validateName(name, "الاسم")
        if (!nameVal.isValid) {
            nameError = nameVal.errorMessage
        } else {
            val phoneVal = Validators.validateYemenPhone(phone)
            if (!phoneVal.isValid) {
                phoneError = phoneVal.errorMessage
            } else if (selectedCategory.isEmpty()) {
                scope.launch { snackbarHostState.showSnackbar("يرجى اختيار القسم والتخصص الفني") }
            } else {
                formViewModel.setSubmitting(true)
                formViewModel.setFormState(FormUiState.Loading(stageMessage = "جاري رفع المستندات وتقديم طلب الانضمام..."))
                isUploadingPhotos = photosUris.isNotEmpty()
                val cleanPhone = if (phone.trim().length == 9) phone.trim() else "77${phone.trim()}"

                scope.launch {
                    try {
                        val uploadedUrls = formViewModel.uploadImages(context, photosUris) { idx ->
                            FirebaseStorageUploader.getProviderWorkPhotoPath(cleanPhone, idx)
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

                        formViewModel.setFormState(FormUiState.Success(requestId = cleanPhone, message = "⏳ تم إرسال طلب انضمامك بنجاح! وهو قيد المراجعة الإدارية."))
                        snackbarHostState.showSnackbar("⏳ تم إرسال طلب انضمامك بنجاح! وهو قيد المراجعة الإدارية.")
                        viewModel.setJoinRequestPhone(context, cleanPhone)
                    } catch (e: Exception) {
                        formViewModel.setFormState(FormUiState.Error(e.message ?: "فشل تقديم طلب الانضمام"))
                    } finally {
                        formViewModel.setSubmitting(false)
                        isUploadingPhotos = false
                    }
                }
            }
        }
    }

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
            label = "المحافظة/المدينة الرئيسية *",
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

        FormStateFeedbackView(
            state = formState,
            themeColors = themeColors,
            onRetry = onSubmit,
            onDismissError = { formViewModel.clearError() }
        )

        RegistrationSubmitButton(
            text = "إرسال طلب الانضمام كفني 🛠️",
            onClick = onSubmit,
            isLoading = isSubmitting,
            enabled = termsChecked,
            themeColors = themeColors
        )
    }
}
