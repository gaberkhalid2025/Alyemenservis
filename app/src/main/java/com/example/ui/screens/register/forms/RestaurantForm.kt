package com.example.ui.screens.register.forms

import android.net.Uri
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import com.example.data.StoreEntity
import com.example.ui.MainViewModel
import com.example.ui.screens.register.components.*
import com.example.util.FirebaseStorageUploader
import com.example.util.Validators
import com.example.utils.VisualThemePalette
import kotlinx.coroutines.launch

/**
 * 🍔 RestaurantForm - نموذج تسجيل مطعم / كافيه
 */
@Composable
fun RestaurantForm(
    viewModel: MainViewModel,
    formViewModel: RegistrationFormViewModel,
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

    val isSubmitting by formViewModel.isSubmitting.collectAsState()
    val formState by formViewModel.formState.collectAsState()

    DisposableEffect(Unit) {
        onDispose {
            formViewModel.resetState()
        }
    }

    LaunchedEffect(formState) {
        if (formState is FormUiState.Success) {
            password = ""
        }
    }

    val onSubmit: () -> Unit = {
        val phoneVal = Validators.validateYemenPhone(phone)
        if (!phoneVal.isValid) {
            scope.launch { snackbarHostState.showSnackbar(phoneVal.errorMessage ?: "خطأ برقم الهاتف") }
        } else if (restName.isBlank() || ownerName.isBlank()) {
            scope.launch { snackbarHostState.showSnackbar("يرجى ملء جميع الحقول الإلزامية") }
        } else {
            formViewModel.setSubmitting(true)
            formViewModel.setFormState(FormUiState.Loading(stageMessage = "جاري رفع صور المطعم وتقديم الطلب..."))
            val cleanPhone = if (phone.trim().length == 9) phone.trim() else "77${phone.trim()}"

            scope.launch {
                try {
                    val uploadedUrls = formViewModel.uploadImages(context, photosUris) { idx ->
                        FirebaseStorageUploader.getStorePhotoPath(cleanPhone, idx)
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
                    formViewModel.setFormState(FormUiState.Success(requestId = cleanPhone, message = "⏳ تم تسجيل المطعم بنجاح وهو قيد المراجعة الإدارية!"))
                    snackbarHostState.showSnackbar("⏳ تم تسجيل المطعم بنجاح وهو قيد المراجعة الإدارية!")
                    viewModel.setJoinRequestPhone(context, cleanPhone)
                } catch (e: Exception) {
                    formViewModel.setFormState(FormUiState.Error(e.message ?: "فشل تسجيل طلب المطعم"))
                } finally {
                    formViewModel.setSubmitting(false)
                }
            }
        }
    }

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

        FormStateFeedbackView(
            state = formState,
            themeColors = themeColors,
            onRetry = onSubmit,
            onDismissError = { formViewModel.clearError() }
        )

        RegistrationSubmitButton(
            text = "تسجيل المطعم الآن 🍔",
            onClick = onSubmit,
            isLoading = isSubmitting,
            enabled = termsChecked,
            themeColors = themeColors
        )
    }
}
