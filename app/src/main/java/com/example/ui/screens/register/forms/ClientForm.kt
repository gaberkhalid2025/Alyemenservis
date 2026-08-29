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

    val isSubmitting by formViewModel.isSubmitting.collectAsState()
    val formState by formViewModel.formState.collectAsState()
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
            } else if (!termsChecked) {
                scope.launch { snackbarHostState.showSnackbar("يرجى الموافقة على شروط الاستخدام أولاً") }
            } else {
                formViewModel.setSubmitting(true)
                formViewModel.setFormState(FormUiState.Loading(stageMessage = "جاري تسجيل حساب العميل..."))
                val cleanPhone = if (phone.trim().length == 9) phone.trim() else "77${phone.trim()}"
                try {
                    viewModel.registerGuestUser(context, name.trim(), cleanPhone, residence.trim(), password.trim())
                    formViewModel.setFormState(FormUiState.Success(requestId = "client_${cleanPhone}", message = "🎉 تم تسجيل حسابك بنجاح!"))
                    scope.launch { snackbarHostState.showSnackbar("🎉 تم تسجيل حسابك بنجاح!") }
                } catch (e: Exception) {
                    formViewModel.setFormState(FormUiState.Error(e.message ?: "فشل تسجيل الحساب"))
                } finally {
                    formViewModel.setSubmitting(false)
                }
            }
        }
    }

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

        FormStateFeedbackView(
            state = formState,
            themeColors = themeColors,
            onRetry = onSubmit,
            onDismissError = { formViewModel.clearError() }
        )

        RegistrationSubmitButton(
            text = "إنشاء حساب العميل الآن 🚀",
            onClick = onSubmit,
            isLoading = isSubmitting,
            enabled = termsChecked,
            themeColors = themeColors
        )
    }
}
