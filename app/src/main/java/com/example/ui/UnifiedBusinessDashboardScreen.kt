package com.example.ui

import androidx.compose.runtime.Composable
import com.example.data.BusinessType
import com.example.data.UnifiedBusinessAccount
import com.example.ui.screens.dashboard.JobPosterDashboard
import com.example.ui.screens.dashboard.MedicalDashboard
import com.example.ui.screens.dashboard.PropertyDashboard
import com.example.ui.screens.dashboard.RestaurantDashboard
import com.example.ui.screens.dashboard.StoreDashboard
import com.example.ui.screens.dashboard.TechnicianDashboard
import com.example.utils.VisualThemePalette

/**
 * 🏢 Clean Unified Delegation Entry Point for Dedicated SaaS Dashboards:
 * 1. Technicians & Service Providers (TechnicianDashboard)
 * 2. Stores & Malls (StoreDashboard)
 * 3. Restaurants & Cafes (RestaurantDashboard)
 * 4. Medical Centers & Clinics (MedicalDashboard)
 * 5. Job Advertisers & Recruiters (JobPosterDashboard)
 * 6. Real Estate Agencies (PropertyDashboard)
 */
@Composable
fun UnifiedBusinessDashboardScreen(
    account: UnifiedBusinessAccount,
    viewModel: MainViewModel,
    themeColors: VisualThemePalette,
    onBackClick: () -> Unit
) {
    when (account.businessType) {
        BusinessType.TECHNICIAN -> TechnicianDashboard(
            account = account,
            viewModel = viewModel,
            themeColors = themeColors,
            onBackClick = onBackClick
        )
        BusinessType.JOB_POSTER -> JobPosterDashboard(
            account = account,
            viewModel = viewModel,
            themeColors = themeColors,
            onBackClick = onBackClick
        )
        BusinessType.REAL_ESTATE -> PropertyDashboard(
            account = account,
            viewModel = viewModel,
            themeColors = themeColors,
            onBackClick = onBackClick
        )
        BusinessType.MEDICAL -> MedicalDashboard(
            account = account,
            viewModel = viewModel,
            themeColors = themeColors,
            onBackClick = onBackClick
        )
        BusinessType.RESTAURANT -> RestaurantDashboard(
            account = account,
            viewModel = viewModel,
            themeColors = themeColors,
            onBackClick = onBackClick
        )
        BusinessType.STORE -> StoreDashboard(
            account = account,
            viewModel = viewModel,
            themeColors = themeColors,
            onBackClick = onBackClick
        )
    }
}
