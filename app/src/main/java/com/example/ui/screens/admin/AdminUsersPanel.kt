package com.example.ui.screens.admin

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.MainViewModel
import com.example.ui.viewmodels.AdminViewModel
import com.example.utils.VisualThemePalette

/**
 * 👤 AdminUsersPanel
 * لوحة إدارة العملاء والمستخدمين وتعديل الصلاحيات والحظر
 */
@Composable
fun AdminUsersPanel(
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    adminViewModel: AdminViewModel = viewModel(),
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    AdminUserManager(
        onBack = onBack,
        adminViewModel = adminViewModel,
        mainViewModel = viewModel,
        themeColors = themeColors,
        modifier = modifier
    )
}
