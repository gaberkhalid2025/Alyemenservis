package com.example.ui.screens.register.forms

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
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
 * 🛠️ ProviderRegistrationForm - نموذج تسجيل الفني / مزود الخدمة
 */
@Composable
fun ProviderRegistrationForm(
    themeColors: VisualThemePalette,
    isLoading: Boolean = false,
    categories: List<Pair<String, String>> = emptyList(), // ID to Name
    onSubmitProvider: (PendingProviderEntity, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var fullName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var profession by remember { mutableStateOf("") }
    var specialization by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("صنعاء") }
    var neighborhood by remember { mutableStateOf("") }
    var idPhoto by remember { mutableStateOf("") }
    var selfiePhoto by remember { mutableStateOf("") }
    var agreeToTerms by remember { mutableStateOf(true) }

    var nameError by remember { mutableStateOf<String?>(null) }
    var phoneError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var professionError by remember { mutableStateOf<String?>(null) }

    fun validate(): Boolean {
        var isValid = true
        val names = fullName.trim().split("\\s+".toRegex())
        if (names.size < 3) {
            nameError = "يرجى كتابة الاسم الثلاثي كاملاً"
            isValid = false
        } else {
            nameError = null
        }

        val cleanPhone = phone.trim().replace("+967", "").replace(" ", "")
        if (cleanPhone.length != 9 || !cleanPhone.startsWith("7")) {
            phoneError = "رقم الهاتف يجب أن يتكون من 9 أرقام ويبدأ بـ 7"
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

        if (profession.isBlank()) {
            professionError = "يرجى تحديد المهنة أو التخصص"
            isValid = false
        } else {
            professionError = null
        }

        if (!agreeToTerms) {
            SnackbarManager.showSnackbar("يرجى الموافقة على شروط الانضمام", SnackbarType.WARNING)
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
            title = "بيانات الفني ومزود الخدمة",
            subtitle = "سجل مهنتك ليصلك طلبات العملاء المباشرة في مدينتك",
            themeColors = themeColors
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                RegistrationField(
                    value = fullName,
                    onValueChange = { fullName = it; if (nameError != null) nameError = null },
                    label = "الاسم الثلاثي للخدمة أو الفني *",
                    placeholder = "مثال: مهندس أحمد الخولاني",
                    leadingIcon = Icons.Default.Person,
                    errorMessage = nameError,
                    themeColors = themeColors
                )

                RegistrationField(
                    value = phone,
                    onValueChange = { phone = it; if (phoneError != null) phoneError = null },
                    label = "رقم الواتساب / الهاتف اليمني *",
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

                RegistrationField(
                    value = profession,
                    onValueChange = { profession = it; if (professionError != null) professionError = null },
                    label = "المهنة / التخصص الرئيسي *",
                    placeholder = "مثال: صيانة كهرباء منازل، سباكة، تكييف",
                    leadingIcon = Icons.Default.Build,
                    errorMessage = professionError,
                    themeColors = themeColors
                )

                RegistrationField(
                    value = specialization,
                    onValueChange = { specialization = it },
                    label = "تفاصيل التخصص والخبرات",
                    placeholder = "مثال: خبرة 5 سنوات في التأسيس والتمديدات",
                    leadingIcon = Icons.Default.Build,
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
                        label = "منطقة العمل الرئيسي",
                        placeholder = "السبعين / حدة",
                        leadingIcon = Icons.Default.Home,
                        themeColors = themeColors,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        RegistrationSection(
            title = "وثائق التوثيق والهوية",
            subtitle = "أرفق صورة البطاقة لضمان التوثيق المعتمد وتفعيل شارة الموثوقية",
            themeColors = themeColors
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                RegistrationImagePicker(
                    label = "صورة الهوية الشخصية / البطاقة",
                    selectedImage = idPhoto,
                    onImageSelected = { idPhoto = it },
                    themeColors = themeColors
                )

                RegistrationImagePicker(
                    label = "صورة شخصية حديثة (Selfie)",
                    selectedImage = selfiePhoto,
                    onImageSelected = { selfiePhoto = it },
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
            text = "إرسال طلب الانضمام كفني معتمد",
            onClick = {
                if (validate()) {
                    val entity = PendingProviderEntity(
                        name = fullName.trim(),
                        phone = phone.trim(),
                        profession = profession.trim(),
                        specialization = specialization.trim(),
                        area = city.trim(),
                        localNeighborhood = neighborhood.trim(),
                        idPhotoBase64 = idPhoto,
                        selfiePhotoBase64 = selfiePhoto,
                        status = "PENDING",
                        password = password.trim(),
                        providerType = "PROVIDER"
                    )
                    onSubmitProvider(entity, password.trim())
                }
            },
            isLoading = isLoading,
            themeColors = themeColors
        )
    }
}
