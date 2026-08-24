package com.example.data

import androidx.annotation.Keep

@Keep
data class PaymentAdminSettingsEntity(
    val id: String = "main_payment_config",
    val isPaymentSystemEnabled: Boolean = true,
    val linkBookings: Boolean = true,
    val linkStores: Boolean = true,
    val linkRestaurants: Boolean = true,
    val linkMedical: Boolean = true,
    val linkProperties: Boolean = true,
    val linkJobs: Boolean = true
)
