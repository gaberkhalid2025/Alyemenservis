package com.example.ui.screens.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.SnackbarHost
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.ui.*
import com.example.utils.PermissionGuard
import com.example.utils.VisualThemePalette

// ✨ المنسق الرئيسي للوحة تحكم الإدارة والمالك والمشرفين
@Composable
fun AdminPanelLayout(
    viewModel: MainViewModel,
    themeColors: VisualThemePalette
) {
    AdminPanelLayoutContent(viewModel = viewModel, themeColors = themeColors)
}

@Composable
private fun AdminPanelLayoutContent(
    viewModel: MainViewModel,
    themeColors: VisualThemePalette
) {
    val state = rememberAdminPanelState(viewModel)

    val allTabs = remember(state.adminRole, state.supervisorPermissions) {
        buildAdminTabsForRole(state.adminRole, state.supervisorPermissions)
    }

    LaunchedEffect(state.adminRole, state.supervisorPermissions, allTabs) {
        if (state.adminRole != "OWNER" && state.activeSubTab == "BACKDOOR") {
            state.activeSubTab = allTabs.firstOrNull()?.first ?: "REG_REQ"
        } else if (allTabs.isNotEmpty() && allTabs.none { it.first == state.activeSubTab }) {
            state.activeSubTab = allTabs.first().first
        }
    }

    if (!state.isAuthorized) {
        AdminSecureLoginScreen(
            viewModel = viewModel,
            themeColors = themeColors,
            context = state.context,
            onAuthenticated = { _, defaultTab ->
                state.isAuthorized = true
                state.activeSubTab = defaultTab
            }
        )
        return
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            item {
                AdminDashboardTopBar(
                    adminRole = state.adminRole,
                    themeColors = themeColors,
                    onLogout = {
                        state.isAuthorized = false
                        viewModel.logout(state.context)
                    }
                )
            }

            item {
                AdminNavigationTabsBar(
                    allTabs = allTabs,
                    selectedGroupFilter = state.selectedGroupFilter,
                    activeSubTab = state.activeSubTab,
                    themeColors = themeColors,
                    onGroupSelected = { state.selectedGroupFilter = it },
                    onTabSelected = { state.activeSubTab = it }
                )
            }

            if (!hasAdminTabAccess(state.activeSubTab, state.adminRole, state.supervisorPermissions)) {
                item {
                    PermissionGuard.UnauthorizedView(
                        customMessage = "ليس لديك صلاحية للوصول إلى هذا القسم الإداري: ${state.activeSubTab}"
                    )
                }
            } else {
                adminSectionCustomization(state.activeSubTab, state, viewModel, themeColors)
                adminSectionEntitiesAndCreation(state.activeSubTab, state, viewModel, themeColors)
                adminSectionOperations(state.activeSubTab, state, viewModel, themeColors)
                adminSectionCommsAndEngagement(state.activeSubTab, state, viewModel, themeColors)
                adminSectionPaymentsAndWallets(state.activeSubTab, state, viewModel, themeColors)
                adminSectionSecurityAndSystem(state.activeSubTab, state, viewModel, themeColors)
            }
        }

        AdminPanelDialogs(
            state = state,
            viewModel = viewModel,
            themeColors = themeColors
        )

        SnackbarHost(
            hostState = state.snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}
