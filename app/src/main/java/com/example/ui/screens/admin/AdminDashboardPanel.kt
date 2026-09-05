package com.example.ui.screens.admin

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.MainViewModel
import com.example.ui.viewmodels.AdminViewModel
import com.example.utils.VisualThemePalette

/**
 * 📊 AdminDashboardPanel
 * لوحة المعلومات والإحصائيات الرئيسية ومؤشرات الأداء العامة
 */
@Composable
fun AdminDashboardPanel(
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    adminViewModel: AdminViewModel = viewModel(),
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    AdminAnalyticsPanel(
        onBack = onBack,
        adminViewModel = adminViewModel,
        modifier = modifier
    )
}
