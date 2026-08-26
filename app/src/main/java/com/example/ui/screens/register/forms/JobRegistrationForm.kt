package com.example.ui.screens.register.forms

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Star
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
 * 💼 JobRegistrationForm - نموذج تسجيل معلن الوظائف والشركات
 */
@Composable
fun JobRegistrationForm(
    themeColors: VisualThemePalette,
    isLoading: Boolean = false,
    onSubmitJob: (PendingProviderEntity, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var companyOrAdvertiserName by remember { mutableStateOf("") }
    var jobSector by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("صنعاء") }
    var neighborhood by remember { mutableStateOf("") }
    var companyLogo by remember { mutableStateOf("") }
    var licensePhoto by remember { mutableStateOf("") }
    var agreeToTerms by remember { mutableStateOf(true) }

    var nameError by remember { mutableStateOf<String?>(null) }
    var phoneError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }

    fun validate(): Boolean {
        var isValid = true
        if (companyOrAdvertiserName.trim().length < 2) {
            nameError = "يرجى كتابة اسم الشركة أو جهة التوظيف"
            isValid = false
        } else {
            nameError = null
        }

        val cleanPhone = phone.trim().replace("+967", "").replace(" ", "")
        if (cleanPhone.length != 9 || !cleanPhone.startsWith("7")) {
            phoneError = "رقم التواصل يجب أن يبدأ بـ 7 ومكون من 9 أرقام"
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
            SnackbarManager.showSnackbar("يرجى الموافقة على الشروط لنشر إعلانات الوظائف", SnackbarType.WARNING)
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
            title = "بيانات جهة التوظيف / المعلن",
            subtitle = "انشر الشواغر والفرص الوظيفية واستقبل طلبات المتقدمين المباشرة",
            themeColors = themeColors
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                RegistrationField(
                    value = companyOrAdvertiserName,
                    onValueChange = { companyOrAdvertiserName = it; if (nameError != null) nameError = null },
                    label = "اسم الشركة / المؤسسة / المعلن *",
                    placeholder = "مثال: شركة النجم للتكنولوجيا / مسؤول التوظيف",
                    leadingIcon = Icons.Default.Person,
                    errorMessage = nameError,
                    themeColors = themeColors
                )

                RegistrationField(
                    value = jobSector,
                    onValueChange = { jobSector = it },
                    label = "قطاع الأعمال / مجال الوظائف",
                    placeholder = "مثال: مبيعات، تسويق، برمجة، إدارة، محاسبة",
                    leadingIcon = Icons.Default.Star,
                    themeColors = themeColors
                )

                RegistrationField(
                    value = phone,
                    onValueChange = { phone = it; if (phoneError != null) phoneError = null },
                    label = "رقم هاتف التواصل والواتساب *",
                    placeholder = "771234567",
                    leadingIcon = Icons.Default.Phone,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    errorMessage = phoneError,
                    themeColors = themeColors
                )

                RegistrationField(
                    value = password,
                    onValueChange = { password = it; if (passwordError != null) passwordError = null },
                    label = "كلمة المرور للحساب *",
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
                        label = "المنطقة / الفرع",
                        placeholder = "شارع حدة",
                        leadingIcon = Icons.Default.Home,
                        themeColors = themeColors,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        RegistrationSection(
            title = "الشعار وتوثيق الشركة",
            subtitle = "رفع شعار الشركة والترخيص التجاري لتوثيق الحساب",
            themeColors = themeColors
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                RegistrationImagePicker(
                    label = "شعار الشركة / جهة التوظيف",
                    selectedImage = companyLogo,
                    onImageSelected = { companyLogo = it },
                    themeColors = themeColors
                )

                RegistrationImagePicker(
                    label = "صورة السجل التجاري / الهوية",
                    selectedImage = licensePhoto,
                    onImageSelected = { licensePhoto = it },
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
            text = "تسجيل معلن الوظائف ونشر الشواغر",
            onClick = {
                if (validate()) {
                    val entity = PendingProviderEntity(
                        name = companyOrAdvertiserName.trim(),
                        phone = phone.trim(),
                        profession = jobSector.ifBlank { "إعلانات وظائف وتوظيف" },
                        area = city.trim(),
                        localNeighborhood = neighborhood.trim(),
                        idPhotoBase64 = licensePhoto,
                        selfiePhotoBase64 = companyLogo,
                        status = "PENDING",
                        password = password.trim(),
                        providerType = "JOB"
                    )
                    onSubmitJob(entity, password.trim())
                }
            },
            isLoading = isLoading,
            themeColors = themeColors
        )
    }
}
