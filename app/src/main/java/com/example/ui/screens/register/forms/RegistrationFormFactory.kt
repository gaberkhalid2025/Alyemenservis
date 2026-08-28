package com.example.ui.screens.register.forms

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import com.example.data.CategoryEntity
import com.example.ui.MainViewModel
import com.example.ui.screens.register.RegistrationType
import com.example.utils.VisualThemePalette

/**
 * 🏭 RegistrationFormFactory - مصنع النماذج لتوليد نموذج التسجيل المناسب ديناميكياً
 */
object RegistrationFormFactory {

    @Composable
    fun CreateForm(
        type: RegistrationType,
        viewModel: MainViewModel,
        formViewModel: RegistrationFormViewModel,
        themeColors: VisualThemePalette,
        categories: List<CategoryEntity>,
        snackbarHostState: SnackbarHostState,
        initialName: String,
        initialPhone: String,
        initialResidence: String
    ) {
        when (type) {
            RegistrationType.CLIENT -> ClientForm(
                viewModel = viewModel,
                formViewModel = formViewModel,
                themeColors = themeColors,
                snackbarHostState = snackbarHostState,
                initialName = initialName,
                initialPhone = initialPhone,
                initialResidence = initialResidence
            )
            RegistrationType.PROVIDER -> ProviderForm(
                viewModel = viewModel,
                formViewModel = formViewModel,
                themeColors = themeColors,
                categories = categories,
                snackbarHostState = snackbarHostState,
                initialName = initialName,
                initialPhone = initialPhone,
                initialResidence = initialResidence
            )
            RegistrationType.STORE -> StoreForm(
                viewModel = viewModel,
                formViewModel = formViewModel,
                themeColors = themeColors,
                snackbarHostState = snackbarHostState,
                initialName = initialName,
                initialPhone = initialPhone
            )
            RegistrationType.RESTAURANT -> RestaurantForm(
                viewModel = viewModel,
                formViewModel = formViewModel,
                themeColors = themeColors,
                snackbarHostState = snackbarHostState,
                initialName = initialName,
                initialPhone = initialPhone
            )
            RegistrationType.MEDICAL -> MedicalForm(
                viewModel = viewModel,
                formViewModel = formViewModel,
                themeColors = themeColors,
                snackbarHostState = snackbarHostState,
                initialName = initialName,
                initialPhone = initialPhone
            )
            RegistrationType.PROPERTY -> PropertyForm(
                viewModel = viewModel,
                formViewModel = formViewModel,
                themeColors = themeColors,
                snackbarHostState = snackbarHostState,
                initialName = initialName,
                initialPhone = initialPhone
            )
            RegistrationType.JOB -> JobForm(
                viewModel = viewModel,
                formViewModel = formViewModel,
                themeColors = themeColors,
                snackbarHostState = snackbarHostState,
                initialName = initialName,
                initialPhone = initialPhone
            )
        }
    }
}
