package com.example.domain.entities

/**
 * 🏛️ Domain Entities for Dashboards, Products, Favorites, Ratings & Gallery
 * Pure Kotlin entities independent of Android dependencies.
 */

data class DashboardStatsEntity(
    val totalViews: Int = 0,
    val activeBookingsCount: Int = 0,
    val completedBookingsCount: Int = 0,
    val averageRating: Double = 5.0,
    val totalReviewsCount: Int = 0,
    val totalRevenueYer: Double = 0.0,
    val totalProductsCount: Int = 0,
    val pendingRequestsCount: Int = 0
)

data class ProductItemEntity(
    val id: String = "",
    val ownerId: String = "",
    val title: String = "",
    val description: String = "",
    val category: String = "",
    val priceYer: Double = 0.0,
    val imageUrl: String = "",
    val isAvailable: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

data class FavoriteItemEntity(
    val id: String = "",
    val userId: String = "",
    val targetId: String = "",
    val targetType: String = "", // PROVIDER, STORE, RESTAURANT, MEDICAL, PROPERTY, JOB
    val title: String = "",
    val category: String = "",
    val city: String = "",
    val imageUrl: String = "",
    val rating: Double = 5.0,
    val createdAt: Long = System.currentTimeMillis()
)

data class RatingReviewEntity(
    val id: String = "",
    val targetId: String = "",
    val authorName: String = "",
    val authorPhone: String = "",
    val rating: Double = 5.0,
    val comment: String = "",
    val dateTimestamp: Long = System.currentTimeMillis()
)

data class GalleryAlbumEntity(
    val id: String = "",
    val ownerId: String = "",
    val title: String = "",
    val imageUrls: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
)
