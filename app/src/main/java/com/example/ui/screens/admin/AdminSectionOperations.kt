package com.example.ui.screens.admin

import androidx.compose.foundation.lazy.LazyListScope
import com.example.ui.MainViewModel
import com.example.utils.VisualThemePalette

fun LazyListScope.adminSectionOperations(
    activeSubTab: String,
    state: AdminPanelState,
    viewModel: MainViewModel,
    themeColors: VisualThemePalette
) {
    when (activeSubTab) {
        "BOOKINGS" -> adminBookingsManagementTab(state, viewModel, themeColors)
        "NOTIFICATIONS" -> adminNotificationsManagementTab(state, viewModel, themeColors)
        "VIP" -> adminVipSubscriptionsTab(state, viewModel, themeColors)
    }
}
