package com.example.data.models

import androidx.annotation.Keep

@Keep
data class BookingCore(
    val id: String = "",
    val bookingNumber: String = "",
    val bookingCode: String = "",
    val status: String = "PENDING",
    val clientId: String = "",
    val clientName: String = "",
    val clientPhone: String = "",
    val clientAddress: String = ""
)

@Keep
data class BookingService(
    val serviceType: String = "",
    val category: String = "",
    val subCategory: String = "",
    val serviceDetails: String = "",
    val providerId: String = "",
    val providerName: String = "",
    val providerPhone: String = ""
)

@Keep
data class BookingSchedule(
    val dateString: String = "",
    val timeString: String = "",
    val date: String = "",
    val time: String = "",
    val scheduledAt: Long = 0L
)

@Keep
data class BookingSecurity(
    val pinCode: String = "",
    val bookingPassword: String = "",
    val rejectionReason: String = ""
)
