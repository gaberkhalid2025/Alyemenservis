package com.example.data

import androidx.annotation.Keep

@Keep
data class RequestOfferEntity(
    val id: String = "",
    val requestId: String = "",
    val requestCode: String = "",
    val technicianId: String = "",
    val technicianName: String = "",
    val technicianPhone: String = "",
    val technicianAvatar: String = "",
    val technicianRating: Float = 5.0f,
    val price: Double = 0.0,
    val estimatedArrivalTime: String = "خلال 30 دقيقة",
    val estimatedDuration: String = "ساعتان",
    val notes: String = "",
    val status: String = "PENDING", // PENDING, ACCEPTED, REJECTED
    val technicianLatitude: Double = 15.3694,
    val technicianLongitude: Double = 44.1910,
    val distanceKm: Double = 2.5,
    val createdAt: Long = System.currentTimeMillis()
)
