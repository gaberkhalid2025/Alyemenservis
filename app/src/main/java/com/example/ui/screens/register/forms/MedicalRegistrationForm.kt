package com.example.ui.screens.register.forms

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.PendingProviderEntity
import com.example.ui.components.SnackbarManager
import com.example.ui.components.SnackbarType
import com.example.ui.screens.register.components.RegistrationField
import com.example.ui.screens.register.components.RegistrationImagePicker
import com.example.ui.screens.register.components.RegistrationSection
import com.example.ui.screens.register.components.RegistrationSubmitButton
import com.example.ui.screens.register.components.RegistrationTermsCheckbox
import com.example.utils.VisualThemePalette

/**
 * 🏥 MedicalRegistrationForm - نموذج تسجيل المركز الطبي / العيادة / الصيدلية / الطبيب
 */
@Composable
fun MedicalRegistrationForm(
    themeColors: VisualThemePalette,
    isLoading: Boolean = false,
    onSubmitMedical: (PendingProviderEntity, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var facilityName by remember { mutableStateOf("") }
    var medicalType by remember { mutableStateOf("عيادة طبية") }
    var specialty by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("صنعاء") }
    var neighborhood by remember { mutableStateOf("") }
    var licensePhoto by remember { mutableStateOf("") }
    var facilityLogo by remember { mutableStateOf("") }
    var agreeToTerms by remember { mutableStateOf(true) }

    var nameError by remember { mutableStateOf<String?>(null) }
    var phoneError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }

    fun validate(): Boolean {
        var isValid = true
        if (facilityName.trim().length < 2) {
            nameError = "يرجى كتابة اسم المنشأة الطبية أو الطبيب"
            isValid = false
        } else {
            nameError = null
        }

        val cleanPhone = phone.trim().replace("+967", "").replace(" ", "")
        if (cleanPhone.length != 9 || !cleanPhone.startsWith("7")) {
            phoneError = "رقم هاتف التواصل يجب أن يبدأ بـ 7 ومكون من 9 أرقام"
            isValid = false
        } else {
            phoneError = null
        }

        if (password.length < 6) {
            passwordError = "كلمة المرور يجب أن لا تقل عن 6 أحرف"
            isValid = false
        } else {
            passwordError = null
        }

        if (!agreeToTerms) {
            SnackbarManager.showSnackbar("يرجى الموافقة على شروط التسجيل الطبي", SnackbarType.WARNING)
            isValid = false
        }

        return isValid
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        RegistrationSection(
            title = "بيانات المركز الطبي / العيادة",
            subtitle = "أدرج خدماتك الطبية ومواعيد الاستشارات وحجوزات المرضى",
            themeColors = themeColors
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                RegistrationField(
                    value = facilityName,
                    onValueChange = { facilityName = it; if (nameError != null) nameError = null },
                    label = "اسم المنشأة الطبية / الطبيب *",
                    placeholder = "مثال: مجمع الشفاء الطبي / د. أحمد العلي",
                    leadingIcon = Icons.Default.Person,
                    errorMessage = nameError,
                    themeColors = themeColors
                )

                RegistrationField(
                    value = specialty,
                    onValueChange = { specialty = it },
                    label = "التخصص الطبي والتفاصيل",
                    placeholder = "مثال: طب وجراحة العيون، أطفال، صيدلية، مختبرات",
                    leadingIcon = Icons.Default.Person,
                    themeColors = themeColors
                )

                RegistrationField(
                    value = phone,
                    onValueChange = { phone = it; if (phoneError != null) phoneError = null },
                    label = "رقم هاتف الحجوزات والواتساب *",
                    placeholder = "771234567",
                    leadingIcon = Icons.Default.Phone,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    errorMessage = phoneError,
                    themeColors = themeColors
                )

                RegistrationField(
                    value = password,
                    onValueChange = { password = it; if (passwordError != null) passwordError = null },
                    label = "كلمة المرور للحساب الطبي *",
                    placeholder = "******",
                    leadingIcon = Icons.Default.Lock,
                    isPassword = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    errorMessage = passwordError,
                    themeColors = themeColors
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RegistrationField(
                        value = city,
                        onValueChange = { city = it },
                        label = "المدينة *",
                        placeholder = "صنعاء",
                        leadingIcon = Icons.Default.Place,
                        themeColors = themeColors,
                        modifier = Modifier.weight(1f)
                    )

                    RegistrationField(
                        value = neighborhood,
                        onValueChange = { neighborhood = it },
                        label = "الشارع / المنطقة",
                        placeholder = "شارع الزبيري",
                        leadingIcon = Icons.Default.Home,
                        themeColors = themeColors,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        RegistrationSection(
            title = "الترخيص والتصريح الطبي",
            subtitle = "أرفق ترخيص مزاولة المهنة لتوثيق الحساب بالشارة المعتمدة",
            themeColors = themeColors
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                RegistrationImagePicker(
                    label = "صورة ترخيص مزاولة المهنة / ترخيص المنشأة",
                    selectedImage = licensePhoto,
                    onImageSelected = { licensePhoto = it },
                    themeColors = themeColors
                )

                RegistrationImagePicker(
                    label = "شعار المركز / العيادة",
                    selectedImage = facilityLogo,
                    onImageSelected = { facilityLogo = it },
                    themeColors = themeColors
                )
            }
        }

        RegistrationTermsCheckbox(
            checked = agreeToTerms,
            onCheckedChange = { agreeToTerms = it },
            themeColors = themeColors
        )

        RegistrationSubmitButton(
            text = "تسجيل المنشأة الطبية وتفعيل الحجوزات",
            onClick = {
                if (validate()) {
                    val entity = PendingProviderEntity(
                        name = facilityName.trim(),
                        phone = phone.trim(),
                        profession = specialty.ifBlank { "خدمات طبية وعيادات" },
                        area = city.trim(),
                        localNeighborhood = neighborhood.trim(),
                        idPhotoBase64 = licensePhoto,
                        selfiePhotoBase64 = facilityLogo,
                        status = "PENDING",
                        password = password.trim(),
                        providerType = "MEDICAL"
                    )
                    onSubmitMedical(entity, password.trim())
                }
            },
            isLoading = isLoading,
            themeColors = themeColors
        )
    }
}
