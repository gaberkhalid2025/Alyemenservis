package com.example.data

import androidx.annotation.Keep

@Keep
data class RatingEntity(
    val id: String = "",
    val targetId: String = "", // storeId or propertyId
    val targetType: String = "STORE", // STORE, PROPERTY
    val userId: String = "",
    val userName: String = "",
    val rating: Float = 5.0f,
    val comment: String = "",
    val isApproved: Boolean = true,
    val timestamp: Long = System.currentTimeMillis(),
    val reply: String = "",
    val replyTimestamp: Long? = null
)
