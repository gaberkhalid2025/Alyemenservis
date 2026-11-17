package com.example.ui.screens.admin

import androidx.compose.runtime.Composable
import com.example.ui.MainViewModel
import com.example.utils.VisualThemePalette

@Composable
fun AdminPanelDialogs(
    state: AdminPanelState,
    viewModel: MainViewModel,
    themeColors: VisualThemePalette
) {
    AdminCatalogAndRequestDialogs(
        state = state,
        viewModel = viewModel,
        themeColors = themeColors
    )
    AdminOperationsAndBookingDialogs(
        state = state,
        viewModel = viewModel,
        themeColors = themeColors
    )
    AdminPaymentAndWalletDialogs(
        state = state,
        viewModel = viewModel,
        themeColors = themeColors
    )
    AdminSecurityAndSupervisorDialogs(
        state = state,
        viewModel = viewModel,
        themeColors = themeColors
    )
}
