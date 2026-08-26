package com.example.data

import androidx.annotation.Keep

@Keep
data class InstantRequestEntity(
    val id: String = "",
    val requestCode: String = "", // e.g. R-942338
    val secretPin: String = "", // 6-digit PIN for customer verification
    val cancellationPassword: String = "", // 4-digit cancellation code
    val userId: String = "",
    val userName: String = "",
    val userPhone: String = "",
    val userCity: String = "",
    val userNeighborhood: String = "",
    val categoryId: String = "services",
    val categoryName: String = "",
    val serviceTitle: String = "",
    val serviceDetails: String = "",
    val description: String = "",
    val images: List<String> = emptyList(),
    val status: String = "WAITING_FOR_OFFERS", // WAITING_FOR_OFFERS, REVIEWING_OFFERS, ACCEPTED, IN_PROGRESS, COMPLETED, EXPIRED, CANCELLED
    val acceptedOfferId: String = "",
    val acceptedTechnicianId: String = "",
    val acceptedTechnicianName: String = "",
    val acceptedTechnicianPhone: String = "",
    val acceptedPrice: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis(),
    val expiresAt: Long = System.currentTimeMillis() + 30 * 60 * 1000L, // 30 minutes
    val offersCount: Int = 0,
    val deliveryMethod: String = "",
    val urgencyTime: String = "فوراً (خلال 30 دقيقة)"
)
