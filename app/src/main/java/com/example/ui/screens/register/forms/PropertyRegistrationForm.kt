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
 * 🏢 PropertyRegistrationForm - نموذج تسجيل العروض العقارية ومكتب العقارات
 */
@Composable
fun PropertyRegistrationForm(
    themeColors: VisualThemePalette,
    isLoading: Boolean = false,
    onSubmitProperty: (PendingProviderEntity, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var ownerOrOfficeName by remember { mutableStateOf("") }
    var propertyType by remember { mutableStateOf("شقة للإيجار") }
    var priceText by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("صنعاء") }
    var neighborhood by remember { mutableStateOf("") }
    var propertyPhoto by remember { mutableStateOf("") }
    var idPhoto by remember { mutableStateOf("") }
    var agreeToTerms by remember { mutableStateOf(true) }

    var nameError by remember { mutableStateOf<String?>(null) }
    var phoneError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }

    fun validate(): Boolean {
        var isValid = true
        if (ownerOrOfficeName.trim().length < 2) {
            nameError = "يرجى كتابة اسم المالك أو المكتب العقاري"
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
            SnackbarManager.showSnackbar("يرجى الموافقة على الشروط لإضافة العرض العقاري", SnackbarType.WARNING)
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
            title = "بيانات العرض والمكتب العقاري",
            subtitle = "سجل عقارك ليصل للمشترين والمستأجرين في مدينتك",
            themeColors = themeColors
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                RegistrationField(
                    value = ownerOrOfficeName,
                    onValueChange = { ownerOrOfficeName = it; if (nameError != null) nameError = null },
                    label = "اسم المكتب العقاري / المالك *",
                    placeholder = "مثال: مكتب الفرسان العقاري / أبو محمد",
                    leadingIcon = Icons.Default.Person,
                    errorMessage = nameError,
                    themeColors = themeColors
                )

                RegistrationField(
                    value = propertyType,
                    onValueChange = { propertyType = it },
                    label = "نوع العقار / الخدمة",
                    placeholder = "مثال: شقق للإيجار، أراضي للبيع، فلل، محلات تجارية",
                    leadingIcon = Icons.Default.Home,
                    themeColors = themeColors
                )

                RegistrationField(
                    value = priceText,
                    onValueChange = { priceText = it },
                    label = "متوسط الأسعار / نطاق الإيجار",
                    placeholder = "مثال: 150,000 ريال يمني / شهر",
                    leadingIcon = Icons.Default.Home,
                    themeColors = themeColors
                )

                RegistrationField(
                    value = phone,
                    onValueChange = { phone = it; if (phoneError != null) phoneError = null },
                    label = "رقم الهاتف للتواصل والواتساب *",
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
                        label = "المنطقة / الحي",
                        placeholder = "الأصبحي",
                        leadingIcon = Icons.Default.Home,
                        themeColors = themeColors,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        RegistrationSection(
            title = "صور العقار والتوثيق",
            subtitle = "أرفق صورة للعقار أو ترخيص المكتب العقاري",
            themeColors = themeColors
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                RegistrationImagePicker(
                    label = "صورة رئيسية للعقار / واجهة المكتب",
                    selectedImage = propertyPhoto,
                    onImageSelected = { propertyPhoto = it },
                    themeColors = themeColors
                )

                RegistrationImagePicker(
                    label = "صورة الهوية / ترخيص المكتب (إن وجد)",
                    selectedImage = idPhoto,
                    onImageSelected = { idPhoto = it },
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
            text = "تسجيل العارض العقاري وتفعيل الإعلانات",
            onClick = {
                if (validate()) {
                    val entity = PendingProviderEntity(
                        name = ownerOrOfficeName.trim(),
                        phone = phone.trim(),
                        profession = propertyType.ifBlank { "خدمات وعروض عقارية" },
                        area = city.trim(),
                        localNeighborhood = neighborhood.trim(),
                        idPhotoBase64 = idPhoto,
                        selfiePhotoBase64 = propertyPhoto,
                        status = "PENDING",
                        password = password.trim(),
                        providerType = "PROPERTY"
                    )
                    onSubmitProperty(entity, password.trim())
                }
            },
            isLoading = isLoading,
            themeColors = themeColors
        )
    }
}
