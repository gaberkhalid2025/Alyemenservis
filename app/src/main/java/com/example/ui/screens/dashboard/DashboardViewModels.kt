package com.example.ui.screens.dashboard

import com.example.ui.screens.dashboard.viewmodels.*

sealed class DashboardEvent {
    data class ShowToast(val message: String) : DashboardEvent()
    data class NavigateToDetail(val id: String) : DashboardEvent()
}

// Backward-compatibility exports & typealiases
typealias DashboardUiState = com.example.ui.screens.dashboard.viewmodels.DashboardUiState
typealias TechnicianDashboardViewModel = com.example.ui.screens.dashboard.viewmodels.TechnicianDashboardViewModel
typealias StoreDashboardViewModel = com.example.ui.screens.dashboard.viewmodels.StoreDashboardViewModel
typealias RestaurantDashboardViewModel = com.example.ui.screens.dashboard.viewmodels.RestaurantDashboardViewModel
typealias DoctorItem = com.example.ui.screens.dashboard.viewmodels.DoctorItem
typealias MedicalDashboardViewModel = com.example.ui.screens.dashboard.viewmodels.MedicalDashboardViewModel
typealias PropertyDashboardViewModel = com.example.ui.screens.dashboard.viewmodels.PropertyDashboardViewModel
typealias JobPostItem = com.example.ui.screens.dashboard.viewmodels.JobPostItem
typealias JobPosterDashboardViewModel = com.example.ui.screens.dashboard.viewmodels.JobPosterDashboardViewModel
typealias FavoritesUiState = com.example.ui.screens.dashboard.viewmodels.FavoritesUiState
typealias FavoritesViewModel = com.example.ui.screens.dashboard.viewmodels.FavoritesViewModel
typealias ServicesBrowserUiState = com.example.ui.screens.dashboard.viewmodels.ServicesBrowserUiState
typealias ServicesBrowserViewModel = com.example.ui.screens.dashboard.viewmodels.ServicesBrowserViewModel
