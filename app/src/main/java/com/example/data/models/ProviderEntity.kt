package com.example.data

import androidx.annotation.Keep

@Keep
data class ProviderEntity(
    val id: String = "",
    val name: String = "",
    val phone: String = "",
    val categoryId: String = "",
    val area: String = "",
    val isVip: Boolean = false,
    val subscriptionStatus: String = "PENDING", // e.g., "APPROVED"
    val isAvailable: Boolean = true,
    val cityId: String = "",
    val localNeighborhood: String = "",
    val rating: Float = 5.0f,
    val points: Int = 0,
    val isVerified: Boolean = true,
    val isRecommended: Boolean = true,
    val numReviews: Int = 0,
    val coverImage: String = "",
    val profileImage: String = "",
    val previewPrice: Double = 1500.0,
    val latitude: Double = 15.3694,
    val longitude: Double = 44.1910,
    val subscriptionExpiry: Long = System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000),
    val workPhotosBase64: List<String> = emptyList(),
    val productAttachmentsJson: String = "",
    val specialOffersJson: String = "",
    val customCategoryName: String = "",
    val profession: String = "",
    val specialization: String = "",
    val chatRecipientId: String = "",
    val isBlocked: Boolean = false,
    val isChatDisabled: Boolean = false,
    val isNotificationsDisabled: Boolean = false,
    val isPaymentRequired: Boolean = false,
    val password: String = "",
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null
)

@Keep
data class PendingProviderEntity(
    val id: String = "",
    val name: String = "",
    val phone: String = "",
    val categoryId: String = "",
    val area: String = "",
    val localNeighborhood: String = "",
    val status: String = "PENDING",
    val reason: String = "",
    val idPhotoBase64: String = "",
    val selfiePhotoBase64: String = "",
    val workPhotosBase64: List<String> = emptyList(),
    val productAttachmentsJson: String = "",
    val customCategoryName: String = "",
    val profession: String = "",
    val specialization: String = "",
    val chatRecipientId: String = "",
    val password: String = ""
)
