package com.example.ui.screens.register.forms

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.PropertyEntity
import com.example.ui.MainViewModel
import com.example.ui.screens.register.components.*
import com.example.util.FirebaseStorageUploader
import com.example.util.Validators
import com.example.utils.VisualThemePalette
import kotlinx.coroutines.launch

/**
 * 🏠 PropertyForm - نموذج إدراج عقار للبيع أو الإيجار
 */
@Composable
fun PropertyForm(
    viewModel: MainViewModel,
    formViewModel: RegistrationFormViewModel,
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

    val isSubmitting by formViewModel.isSubmitting.collectAsState()
    val formState by formViewModel.formState.collectAsState()

    DisposableEffect(Unit) {
        onDispose {
            formViewModel.resetState()
        }
    }

    LaunchedEffect(formState) {
        if (formState is FormUiState.Success) {
            // Keep state intact or reset some fields
        }
    }

    val onSubmit: () -> Unit = {
        val phoneVal = Validators.validateYemenPhone(phone)
        if (!phoneVal.isValid) {
            scope.launch { snackbarHostState.showSnackbar(phoneVal.errorMessage ?: "خطأ برقم الهاتف") }
        } else if (propTitle.isBlank() || ownerName.isBlank()) {
            scope.launch { snackbarHostState.showSnackbar("يرجى ملء جميع الحقول الإلزامية") }
        } else {
            formViewModel.setSubmitting(true)
            formViewModel.setFormState(FormUiState.Loading(stageMessage = "جاري رفع صور العقار وإدراجه..."))
            val cleanPhone = if (phone.trim().length == 9) phone.trim() else "77${phone.trim()}"

            scope.launch {
                try {
                    val uploadedUrls = formViewModel.uploadImages(context, photosUris) { idx ->
                        FirebaseStorageUploader.getPropertyPhotoPath(cleanPhone, idx)
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
                    formViewModel.setFormState(FormUiState.Success(requestId = cleanPhone, message = "⏳ تم إدراج العقار بنجاح وهو قيد الاعتماد الظاهر!"))
                    snackbarHostState.showSnackbar("⏳ تم إردراج العقار بنجاح وهو قيد الاعتماد الظاهر!")
                    viewModel.setJoinRequestPhone(context, cleanPhone)
                } catch (e: Exception) {
                    formViewModel.setFormState(FormUiState.Error(e.message ?: "فشل إدراج العقار"))
                } finally {
                    formViewModel.setSubmitting(false)
                }
            }
        }
    }

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

        FormStateFeedbackView(
            state = formState,
            themeColors = themeColors,
            onRetry = onSubmit,
            onDismissError = { formViewModel.clearError() }
        )

        RegistrationSubmitButton(
            text = "إدراج العقار بالدليل 🏡",
            onClick = onSubmit,
            isLoading = isSubmitting,
            enabled = termsChecked,
            themeColors = themeColors
        )
    }
}
