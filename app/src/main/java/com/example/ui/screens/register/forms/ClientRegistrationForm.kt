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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.UserEntity
import com.example.ui.components.SnackbarManager
import com.example.ui.components.SnackbarType
import com.example.ui.screens.register.components.RegistrationField
import com.example.ui.screens.register.components.RegistrationSection
import com.example.ui.screens.register.components.RegistrationSubmitButton
import com.example.ui.screens.register.components.RegistrationTermsCheckbox
import com.example.utils.VisualThemePalette

/**
 * 👤 ClientRegistrationForm - نموذج تسجيل العميل الجديد
 * تسجّل المستخدم العادي كعميل وتكون حالته PENDING حتى يتم اعتماده من الإدارة.
 */
@Composable
fun ClientRegistrationForm(
    themeColors: VisualThemePalette,
    isLoading: Boolean = false,
    onSubmitClient: (UserEntity, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var fullName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("صنعاء") }
    var neighborhood by remember { mutableStateOf("") }
    var agreeToTerms by remember { mutableStateOf(true) }

    var nameError by remember { mutableStateOf<String?>(null) }
    var phoneError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }

    fun validate(): Boolean {
        var isValid = true
        val names = fullName.trim().split("\\s+".toRegex())
        if (names.size < 3) {
            nameError = "يرجى كتابة الاسم الثلاثي كاملاً (مثال: محمد علي صالح)"
            isValid = false
        } else {
            nameError = null
        }

        val cleanPhone = phone.trim().replace("+967", "").replace(" ", "")
        if (cleanPhone.length != 9 || !cleanPhone.startsWith("7")) {
            phoneError = "رقم الهاتف يجب أن يتكون من 9 أرقام ويبدأ بـ 7 (مثال: 771234567)"
            isValid = false
        } else {
            phoneError = null
        }

        if (password.length < 6) {
            passwordError = "كلمة المرور يجب أن لا تقل عن 6 أحرف أو أرقام"
            isValid = false
        } else {
            passwordError = null
        }

        if (!agreeToTerms) {
            SnackbarManager.showSnackbar("يرجى الموافقة على الشروط والأحكام لإكمال التسجيل", SnackbarType.WARNING)
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
            title = "بيانات العميل الشخصية",
            subtitle = "أدخل بياناتك للتسجيل في دليل خدمات اليمن",
            themeColors = themeColors
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                RegistrationField(
                    value = fullName,
                    onValueChange = {
                        fullName = it
                        if (nameError != null) nameError = null
                    },
                    label = "الاسم الثلاثي كاملاً *",
                    placeholder = "مثال: عبدالملك أحمد الحزمي",
                    leadingIcon = Icons.Default.Person,
                    errorMessage = nameError,
                    themeColors = themeColors
                )

                RegistrationField(
                    value = phone,
                    onValueChange = {
                        phone = it
                        if (phoneError != null) phoneError = null
                    },
                    label = "رقم الهاتف اليمني *",
                    placeholder = "771234567",
                    leadingIcon = Icons.Default.Phone,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    errorMessage = phoneError,
                    themeColors = themeColors
                )

                RegistrationField(
                    value = password,
                    onValueChange = {
                        password = it
                        if (passwordError != null) passwordError = null
                    },
                    label = "كلمة المرور *",
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
                        label = "الحي / المنطقة",
                        placeholder = "مثال: حدة",
                        leadingIcon = Icons.Default.Home,
                        themeColors = themeColors,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // إشعار حالة الانتظار
        Surface(
            color = Color(0xFF1E293B),
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    text = "ℹ️ تنبيه: عند التسجيل سيكون الحساب في حالة (قيد الانتظار) وسيتم تفعيله فور مراجعة وموافقة إدارة تطبيق دليل خدمات اليمن.",
                    color = Color(0xFFCBD5E1),
                    fontSize = 11.5.sp,
                    lineHeight = 16.sp
                )
            }
        }

        RegistrationTermsCheckbox(
            checked = agreeToTerms,
            onCheckedChange = { agreeToTerms = it },
            themeColors = themeColors
        )

        RegistrationSubmitButton(
            text = "إنشاء حساب عميل جديد",
            onClick = {
                if (validate()) {
                    val clientUser = UserEntity(
                        name = fullName.trim(),
                        phone = phone.trim(),
                        city = city.trim(),
                        neighborhood = neighborhood.trim(),
                        role = "CLIENT",
                        verificationStatus = "PENDING"
                    )
                    onSubmitClient(clientUser, password.trim())
                }
            },
            isLoading = isLoading,
            themeColors = themeColors
        )
    }
}
