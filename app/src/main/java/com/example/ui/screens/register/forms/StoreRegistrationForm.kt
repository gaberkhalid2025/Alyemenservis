package com.example.ui.screens.register.forms

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.ShoppingCart
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
 * 🏪 StoreRegistrationForm - نموذج تسجيل المتجر والمحل التجاري
 */
@Composable
fun StoreRegistrationForm(
    themeColors: VisualThemePalette,
    isLoading: Boolean = false,
    onSubmitStore: (PendingProviderEntity, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var storeName by remember { mutableStateOf("") }
    var ownerName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var storeCategory by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("صنعاء") }
    var neighborhood by remember { mutableStateOf("") }
    var licensePhoto by remember { mutableStateOf("") }
    var storeLogo by remember { mutableStateOf("") }
    var agreeToTerms by remember { mutableStateOf(true) }

    var storeNameError by remember { mutableStateOf<String?>(null) }
    var phoneError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }

    fun validate(): Boolean {
        var isValid = true
        if (storeName.trim().length < 2) {
            storeNameError = "يرجى كتابة اسم المتجر بشكل صحيح"
            isValid = false
        } else {
            storeNameError = null
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
            SnackbarManager.showSnackbar("يرجى الموافقة على شروط انضمام المتاجر", SnackbarType.WARNING)
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
            title = "بيانات المتجر التجاري",
            subtitle = "انضم لكبار المتاجر واعرض منتجاتك للعملاء مباشرة",
            themeColors = themeColors
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                RegistrationField(
                    value = storeName,
                    onValueChange = { storeName = it; if (storeNameError != null) storeNameError = null },
                    label = "اسم المتجر / المحل *",
                    placeholder = "مثال: متجر الأمل للإلكترونيات",
                    leadingIcon = Icons.Default.ShoppingCart,
                    errorMessage = storeNameError,
                    themeColors = themeColors
                )

                RegistrationField(
                    value = ownerName,
                    onValueChange = { ownerName = it },
                    label = "اسم مالك المتجر / المسؤول",
                    placeholder = "مثال: علي محمد السعدي",
                    leadingIcon = Icons.Default.Person,
                    themeColors = themeColors
                )

                RegistrationField(
                    value = phone,
                    onValueChange = { phone = it; if (phoneError != null) phoneError = null },
                    label = "رقم هاتف المتجر / الواتساب *",
                    placeholder = "771234567",
                    leadingIcon = Icons.Default.Phone,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    errorMessage = phoneError,
                    themeColors = themeColors
                )

                RegistrationField(
                    value = password,
                    onValueChange = { password = it; if (passwordError != null) passwordError = null },
                    label = "كلمة مرور لوحة تحكم المتجر *",
                    placeholder = "******",
                    leadingIcon = Icons.Default.Lock,
                    isPassword = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    errorMessage = passwordError,
                    themeColors = themeColors
                )

                RegistrationField(
                    value = storeCategory,
                    onValueChange = { storeCategory = it },
                    label = "تصنيف المتجر",
                    placeholder = "مثال: إلكترونيات، ملابس، مواد غذائية، قطع غيار",
                    leadingIcon = Icons.Default.ShoppingCart,
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
                        label = "الشارع / الحي",
                        placeholder = "شارع صخر / حدة",
                        leadingIcon = Icons.Default.Home,
                        themeColors = themeColors,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        RegistrationSection(
            title = "صور وشعار المتجر والتوثيق",
            subtitle = "رفع شعار المتجر والسجل التجاري لتوثيق الحساب",
            themeColors = themeColors
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                RegistrationImagePicker(
                    label = "شعار المتجر / الواجهة",
                    selectedImage = storeLogo,
                    onImageSelected = { storeLogo = it },
                    themeColors = themeColors
                )

                RegistrationImagePicker(
                    label = "صورة السجل التجاري / الترخيص (إن وجد)",
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
            text = "تسجيل المتجر واستلام طلبات الشراء",
            onClick = {
                if (validate()) {
                    val entity = PendingProviderEntity(
                        name = storeName.trim(),
                        phone = phone.trim(),
                        profession = storeCategory.ifBlank { "متجر تجاري" },
                        specialization = "مالك المتجر: ${ownerName.trim()}",
                        area = city.trim(),
                        localNeighborhood = neighborhood.trim(),
                        idPhotoBase64 = licensePhoto,
                        selfiePhotoBase64 = storeLogo,
                        status = "PENDING",
                        password = password.trim(),
                        providerType = "STORE"
                    )
                    onSubmitStore(entity, password.trim())
                }
            },
            isLoading = isLoading,
            themeColors = themeColors
        )
    }
}
