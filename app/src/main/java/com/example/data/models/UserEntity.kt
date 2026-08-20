package com.example.data

import androidx.annotation.Keep

@Keep
data class UserEntity(
    val id: String = "",
    val name: String = "",
    val phone: String = "",
    val email: String = "",
    val city: String = "صنعاء",
    val neighborhood: String = "",
    val role: String = "CLIENT", // CLIENT, PROVIDER, STORE, RESTAURANT, ADMIN
    val isBlocked: Boolean = false,
    val totalBookings: Int = 0,
    val rating: Float = 5.0f,
    val createdAt: Long = System.currentTimeMillis()
)
