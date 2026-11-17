package com.example.ui.screens.admin

import androidx.compose.foundation.lazy.LazyListScope
import com.example.ui.MainViewModel
import com.example.utils.VisualThemePalette

fun LazyListScope.adminSectionSecurityAndSystem(
    activeSubTab: String,
    state: AdminPanelState,
    viewModel: MainViewModel,
    themeColors: VisualThemePalette
) {
    when (activeSubTab) {
        "PASSWORDS_RESET",
        "ROLES_PERMISSIONS",
        "SECURITY_AUDIT" -> {
            adminPasswordResetAndRolesSection(
                activeSubTab = activeSubTab,
                state = state,
                viewModel = viewModel,
                themeColors = themeColors
            )
        }

        "SUPERVISORS" -> {
            adminSupervisorsAndThemesSection(
                state = state,
                viewModel = viewModel,
                themeColors = themeColors
            )
        }

        "BACKUP",
        "CLEAN",
        "STORAGE_QUOTA",
        "DATA_MANAGEMENT" -> {
            adminBackupAndCleanSection(
                activeSubTab = activeSubTab,
                state = state,
                viewModel = viewModel,
                themeColors = themeColors
            )
        }

        else -> {
            adminModerationAndControlSection(
                activeSubTab = activeSubTab,
                state = state,
                viewModel = viewModel,
                themeColors = themeColors
            )
        }
    }
}
