package com.example.ui.screens.register.forms

import android.net.Uri
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Place
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
 * 🏥 MedicalForm - نموذج تسجيل مركز طبي / عيادة / صيدلية
 */
@Composable
fun MedicalForm(
    viewModel: MainViewModel,
    formViewModel: RegistrationFormViewModel,
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
                    val uploadedUrls = formViewModel.uploadImages(context, photosUris) { idx ->
                        FirebaseStorageUploader.getStorePhotoPath(cleanPhone, idx)
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
