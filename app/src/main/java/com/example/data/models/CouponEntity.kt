package com.example.data

import androidx.annotation.Keep

@Keep
data class CouponEntity(
    val id: String = "",
    val code: String = "",
    val pointsValue: Int = 0,
    val expiryTimestamp: Long = 0L,
    val status: String = "ACTIVE",
    val discountPercentage: Int = 0, // percentage discount (e.g. 15 for 15%)
    val maxUsageCount: Int = 100,
    val usedCount: Int = 0
)
