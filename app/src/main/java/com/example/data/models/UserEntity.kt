package com.example.data

import androidx.annotation.Keep

@Keep
data class UserEntity(
    val id: String = "",
    val name: String = "",
    val phone: String = "",
    val email: String = "",
    val isBlocked: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
