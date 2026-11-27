package com.example.ui.screens.admin

import androidx.compose.foundation.lazy.LazyListScope
import com.example.ui.MainViewModel
import com.example.utils.VisualThemePalette

fun LazyListScope.adminSectionEntitiesAndCreation(
    activeSubTab: String,
    state: AdminPanelState,
    viewModel: MainViewModel,
    themeColors: VisualThemePalette
) {
    when (activeSubTab) {
        "MANUAL_ADD" -> adminManualAddTab(state, viewModel, themeColors)
        "PROVIDERS" -> adminProvidersManagementTab(state, viewModel, themeColors)
        "CATEGORIES", "CITIES" -> adminCategoriesAndCitiesTab(activeSubTab, state, viewModel, themeColors)
    }
}
