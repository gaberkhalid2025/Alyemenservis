package com.example.ui.screens.dashboard.viewmodels

import com.example.domain.entities.DashboardStatsEntity
import com.example.domain.entities.GalleryAlbumEntity
import com.example.domain.entities.ProductItemEntity
import com.example.domain.entities.RatingReviewEntity
import com.example.ui.screens.dashboard.DashboardEvent

/**
 * 🎨 Common UI States for Dashboards and Features
 */
data class DashboardUiState(
    val isLoading: Boolean = true,
    val stats: DashboardStatsEntity = DashboardStatsEntity(),
    val products: List<ProductItemEntity> = emptyList(),
    val reviews: List<RatingReviewEntity> = emptyList(),
    val galleryAlbums: List<GalleryAlbumEntity> = emptyList(),
    val activeTab: Int = 0, // 0: Overview, 1: Products/Services, 2: Bookings/Orders, 3: Gallery, 4: Reviews
    val errorMessage: String? = null
)
