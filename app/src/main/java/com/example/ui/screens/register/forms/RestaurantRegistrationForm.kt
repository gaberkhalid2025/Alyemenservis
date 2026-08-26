package com.example.ui.screens.register.forms

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
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
 * 🍔 RestaurantRegistrationForm - نموذج تسجيل المطعم والكافيه
 */
@Composable
fun RestaurantRegistrationForm(
    themeColors: VisualThemePalette,
    isLoading: Boolean = false,
    onSubmitRestaurant: (PendingProviderEntity, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var restaurantName by remember { mutableStateOf("") }
    var cuisineType by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("صنعاء") }
    var neighborhood by remember { mutableStateOf("") }
    var logoPhoto by remember { mutableStateOf("") }
    var healthPermitPhoto by remember { mutableStateOf("") }
    var agreeToTerms by remember { mutableStateOf(true) }

    var nameError by remember { mutableStateOf<String?>(null) }
    var phoneError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }

    fun validate(): Boolean {
        var isValid = true
        if (restaurantName.trim().length < 2) {
            nameError = "يرجى كتابة اسم المطعم أو الكافيه"
            isValid = false
        } else {
            nameError = null
        }

        val cleanPhone = phone.trim().replace("+967", "").replace(" ", "")
        if (cleanPhone.length != 9 || !cleanPhone.startsWith("7")) {
            phoneError = "رقم هاتف الطلبات يجب أن يبدأ بـ 7 ومكون من 9 أرقام"
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
            SnackbarManager.showSnackbar("يرجى الموافقة على الشروط للانضمام", SnackbarType.WARNING)
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
            title = "بيانات المطعم / الكافيه",
            subtitle = "أدرج مطعمك وقائمة الطعام للطلبات والتوصيل السريع",
            themeColors = themeColors
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                RegistrationField(
                    value = restaurantName,
                    onValueChange = { restaurantName = it; if (nameError != null) nameError = null },
                    label = "اسم المطعم / الكافيه *",
                    placeholder = "مثال: مطعم الشيباني الذهبي",
                    leadingIcon = Icons.Default.Star,
                    errorMessage = nameError,
                    themeColors = themeColors
                )

                RegistrationField(
                    value = cuisineType,
                    onValueChange = { cuisineType = it },
                    label = "نوع المأكولات / المطبخ",
                    placeholder = "مثال: مأكولات شعبية، مشويات، وجبات سريعة، حلويات",
                    leadingIcon = Icons.Default.Star,
                    themeColors = themeColors
                )

                RegistrationField(
                    value = phone,
                    onValueChange = { phone = it; if (phoneError != null) phoneError = null },
                    label = "رقم هاتف الطلبات والواتساب *",
                    placeholder = "771234567",
                    leadingIcon = Icons.Default.Phone,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    errorMessage = phoneError,
                    themeColors = themeColors
                )

                RegistrationField(
                    value = password,
                    onValueChange = { password = it; if (passwordError != null) passwordError = null },
                    label = "كلمة مرور حساب المطعم *",
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
                        label = "الشارع / الفرع",
                        placeholder = "شارع حدة",
                        leadingIcon = Icons.Default.Home,
                        themeColors = themeColors,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        RegistrationSection(
            title = "الشعار والتصريح الصحي",
            subtitle = "أرفق شعار المطعم والتصريح الصحي لضمان موثوقية الخدمة",
            themeColors = themeColors
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                RegistrationImagePicker(
                    label = "شعار المطعم / صورة الواجهة",
                    selectedImage = logoPhoto,
                    onImageSelected = { logoPhoto = it },
                    themeColors = themeColors
                )

                RegistrationImagePicker(
                    label = "صورة التصريح الصحي / الرخصة (إن وجدت)",
                    selectedImage = healthPermitPhoto,
                    onImageSelected = { healthPermitPhoto = it },
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
            text = "تسجيل المطعم وتفعيل المنيو الإلكتروني",
            onClick = {
                if (validate()) {
                    val entity = PendingProviderEntity(
                        name = restaurantName.trim(),
                        phone = phone.trim(),
                        profession = cuisineType.ifBlank { "مطعم وكافيه" },
                        area = city.trim(),
                        localNeighborhood = neighborhood.trim(),
                        idPhotoBase64 = healthPermitPhoto,
                        selfiePhotoBase64 = logoPhoto,
                        status = "PENDING",
                        password = password.trim(),
                        providerType = "RESTAURANT"
                    )
                    onSubmitRestaurant(entity, password.trim())
                }
            },
            isLoading = isLoading,
            themeColors = themeColors
        )
    }
}
