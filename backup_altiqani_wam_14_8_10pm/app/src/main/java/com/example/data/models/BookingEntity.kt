package com.example.data

import androidx.annotation.Keep

@Keep
data class BookingEntity(
    val id: String = "",
    val customerName: String = "",
    val customerPhone: String = "",
    val customerArea: String = "",
    val serviceType: String = "",
    val providerId: String = "",
    val providerName: String = "",
    val dateString: String = "",
    val timeString: String = "",
    val status: String = "PENDING", // "PENDING", "APPROVED", "REJECTED"
    val rejectionReason: String = "",
    val pinCode: String = "",
    
    // New Fields requested for the enhanced booking system
    val bookingNumber: String = "",      // BK-YYMMDDHHMMSS-XXXX
    val bookingPassword: String = "",    // 4-digit code (e.g. "8372")
    val clientId: String = "",
    val clientName: String = "",
    val clientPhone: String = "",
    val clientAddress: String = "",
    val providerPhone: String = "",
    val category: String = "",
    val subCategory: String = "",
    val serviceDetails: String = "",
    val date: String = "",
    val time: String = "",
    val advancePayment: Double = 0.0,
    val paymentStatus: String = "unpaid",
    val totalAmount: Double = 0.0,
    val progress: Int = 0,
    
    val cancellationReason: String? = null,
    val cancelledAt: Long? = null,
    val cancelledBy: String? = null,
    
    val requiresPasswordForCancellation: Boolean = true,
    val cancellationAttempts: Int = 0,
    val maxCancellationAttempts: Int = 3,
    val isLocked: Boolean = false,
    val lockedUntil: Long? = null,
    
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val completedAt: Long? = null
)
