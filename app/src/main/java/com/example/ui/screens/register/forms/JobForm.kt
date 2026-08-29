package com.example.ui.screens.register.forms

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import com.example.data.JobEntity
import com.example.ui.MainViewModel
import com.example.ui.screens.register.components.*
import com.example.util.Validators
import com.example.utils.VisualThemePalette
import kotlinx.coroutines.launch

/**
 * 💼 JobForm - نموذج نشر إعلان وظيفة / شاغر شغلي
 */
@Composable
fun JobForm(
    viewModel: MainViewModel,
    formViewModel: RegistrationFormViewModel,
    themeColors: VisualThemePalette,
    snackbarHostState: SnackbarHostState,
    initialName: String,
    initialPhone: String
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var jobTitle by remember { mutableStateOf("") }
    var companyName by remember { mutableStateOf("") }
    var managerName by remember { mutableStateOf(initialName) }
    var phone by remember { mutableStateOf(initialPhone) }
    var city by remember { mutableStateOf("صنعاء") }
    var salary by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var termsChecked by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    RegistrationSection(
        title = "نشر إعلان وظيفة / شاغر شغلي",
        subtitle = "الإعلان عن الشواغر الوظيفية بالمنشأة واستقبال طلبات التوظيف المباشرة",
        icon = "💼",
        themeColors = themeColors
    ) {
        RegistrationField(
            value = jobTitle,
            onValueChange = { jobTitle = it },
            label = "المسمى الوظيفي المطلوب (مثال: محاسب قانوني) *",
            leadingIcon = Icons.Default.Edit,
            themeColors = themeColors
        )

        RegistrationField(
            value = companyName,
            onValueChange = { companyName = it },
            label = "اسم الشركة / المنشأة أو المحل *",
            leadingIcon = Icons.Default.Home,
            themeColors = themeColors
        )

        RegistrationField(
            value = managerName,
            onValueChange = { managerName = it },
            label = "اسم مسؤول التوظيف *",
            leadingIcon = Icons.Default.Person,
            themeColors = themeColors
        )

        RegistrationField(
            value = phone,
            onValueChange = { phone = it },
            label = "رقم هاتف استقبال طلبات التوظيف *",
            placeholder = "771234567",
            leadingIcon = Icons.Default.Phone,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            themeColors = themeColors
        )

        RegistrationField(
            value = city,
            onValueChange = { city = it },
            label = "موقع العمل (المحافظة والحي) *",
            leadingIcon = Icons.Default.Place,
            themeColors = themeColors
        )

        RegistrationField(
            value = salary,
            onValueChange = { salary = it },
            label = "الراتب المتوقع / الشروط المادية *",
            leadingIcon = Icons.Default.Check,
            themeColors = themeColors
        )

        RegistrationField(
            value = description,
            onValueChange = { description = it },
            label = "الوصف الوظيفي والمؤهلات المطلوبة *",
            leadingIcon = Icons.Default.Info,
            singleLine = false,
            minLines = 3,
            maxLines = 5,
            themeColors = themeColors
        )

        RegistrationTermsCheckbox(
            checked = termsChecked,
            onCheckedChange = { termsChecked = it },
            themeColors = themeColors
        )

        RegistrationSubmitButton(
            text = "نشر إعلان الوظيفة الآن 💼",
            onClick = {
                val phoneVal = Validators.validateYemenPhone(phone)
                if (!phoneVal.isValid) {
                    scope.launch { snackbarHostState.showSnackbar(phoneVal.errorMessage ?: "خطأ برقم الهاتف") }
                    return@RegistrationSubmitButton
                }

                isLoading = true
                val cleanPhone = if (phone.trim().length == 9) phone.trim() else "77${phone.trim()}"

                scope.launch {
                    val jobEntity = JobEntity(
                        id = "job_$cleanPhone",
                        title = jobTitle.ifBlank { "شاغر وظيفي - $companyName" },
                        companyName = companyName,
                        managerName = managerName,
                        phone = cleanPhone,
                        cityId = city,
                        salary = salary,
                        description = description
                    )

                    viewModel.saveJob(jobEntity)
                    isLoading = false
                    snackbarHostState.showSnackbar("🎉 تم نشر إعلان الوظيفة بنجاح بالدليل!")
                    viewModel.setJoinRequestPhone(context, cleanPhone)
                }
            },
            isLoading = isLoading,
            enabled = termsChecked,
            themeColors = themeColors
        )
    }
}
