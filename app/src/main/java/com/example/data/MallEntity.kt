package com.example.data

import androidx.annotation.Keep

/**
 * 🏬 MallEntity
 * نموذج بيانات المراكز التجارية والمولات (إدارة الطوابق، المحلات المستأجرة داخل المول، الخريطة الداخلية)
 */
@Keep
data class MallEntity(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val ownerId: String = "",
    val ownerName: String = "",
    val phone: String = "",
    val cityId: String = "",
    val localNeighborhood: String = "",
    val coverImage: String = "",
    val logoImage: String = "",
    val rating: Float = 5.0f,
    val numReviews: Int = 0,
    val isActive: Boolean = true,
    val workingHours: String = "9:00 AM - 11:00 PM",
    val latitude: Double = 15.3694,
    val longitude: Double = 44.1910,
    val floorsCount: Int = 3,
    val storesCount: Int = 0,
    val images: List<String> = emptyList(),
    val floorsDirectoryJson: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
