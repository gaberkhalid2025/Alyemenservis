package com.example.ui.screens.admin

import androidx.compose.foundation.lazy.LazyListScope
import com.example.ui.MainViewModel
import com.example.utils.VisualThemePalette

fun LazyListScope.adminSectionCommsAndEngagement(
    activeSubTab: String,
    state: AdminPanelState,
    viewModel: MainViewModel,
    themeColors: VisualThemePalette
) {
    when (activeSubTab) {
        "ADVANCED_CHAT" -> adminAdvancedChatPanel(viewModel, themeColors)
        "CHATS" -> adminChatManagementTab(state, viewModel, themeColors)
        "BANNERS" -> adminBannersTab(state, viewModel, themeColors)
        "REVIEWS", "CALLS", "COUPONS" -> adminEngagementSubTabs(activeSubTab, state, viewModel, themeColors)
    }
}
