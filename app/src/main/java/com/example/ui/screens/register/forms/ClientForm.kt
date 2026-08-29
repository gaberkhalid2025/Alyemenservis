package com.example.ui.screens.register.forms

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import com.example.ui.MainViewModel
import com.example.ui.screens.register.components.*
import com.example.util.Validators
import com.example.utils.VisualThemePalette
import kotlinx.coroutines.launch

/**
 * 👤 ClientForm - نموذج تسجيل حساب العميل العادي
 */
@Composable
fun ClientForm(
    viewModel: MainViewModel,
    formViewModel: RegistrationFormViewModel,
    themeColors: VisualThemePalette,
    snackbarHostState: SnackbarHostState,
    initialName: String,
    initialPhone: String,
    initialResidence: String
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var name by remember { mutableStateOf(initialName) }
    var phone by remember { mutableStateOf(initialPhone) }
    var residence by remember { mutableStateOf(initialResidence) }
    var password by remember { mutableStateOf("") }
    var termsChecked by remember { mutableStateOf(false) }

    var isLoading by remember { mutableStateOf(false) }
    var phoneError by remember { mutableStateOf<String?>(null) }
    var nameError by remember { mutableStateOf<String?>(null) }

    RegistrationSection(
        title = "بيانات حساب العميل",
        subtitle = "إنشاء حساب للاستفادة من طلب الخدمات المباشرة باليمن",
        icon = "👤",
        themeColors = themeColors
    ) {
        RegistrationField(
            value = name,
            onValueChange = { name = it; nameError = null },
            label = "الاسم الكامل (إجباري) *",
            leadingIcon = Icons.Default.Person,
            errorMessage = nameError,
            themeColors = themeColors
        )

        RegistrationField(
            value = phone,
            onValueChange = { phone = it; phoneError = null },
            label = "رقم الهاتف اليمني (77x / 73x / 71x) *",
            placeholder = "771234567",
            leadingIcon = Icons.Default.Phone,
            errorMessage = phoneError,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            themeColors = themeColors
        )

        RegistrationField(
            value = residence,
            onValueChange = { residence = it },
            label = "المحافظة/المدينة والحي السكني *",
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

        RegistrationTermsCheckbox(
            checked = termsChecked,
            onCheckedChange = { termsChecked = it },
            themeColors = themeColors
        )

        RegistrationSubmitButton(
            text = "إنشاء حساب العميل الآن 🚀",
            onClick = {
                val nameVal = Validators.validateName(name, "الاسم")
                if (!nameVal.isValid) {
                    nameError = nameVal.errorMessage
                    return@RegistrationSubmitButton
                }

                val phoneVal = Validators.validateYemenPhone(phone)
                if (!phoneVal.isValid) {
                    phoneError = phoneVal.errorMessage
                    return@RegistrationSubmitButton
                }

                if (!termsChecked) {
                    scope.launch { snackbarHostState.showSnackbar("يرجى الموافقة على شروط الاستخدام أولاً") }
                    return@RegistrationSubmitButton
                }

                isLoading = true
                val cleanPhone = if (phone.trim().length == 9) phone.trim() else "77${phone.trim()}"
                viewModel.registerGuestUser(context, name.trim(), cleanPhone, residence.trim(), password.trim())
                isLoading = false
                scope.launch { snackbarHostState.showSnackbar("🎉 تم تسجيل حسابك بنجاح!") }
            },
            isLoading = isLoading,
            enabled = termsChecked,
            themeColors = themeColors
        )
    }
}
