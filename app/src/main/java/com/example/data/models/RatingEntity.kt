package com.example.data

import androidx.annotation.Keep

@Keep
data class RatingEntity(
    val id: String = "",
    val targetId: String = "", // providerId, storeId, propertyId
    val targetType: String = "STORE", // PROVIDER, STORE, PROPERTY, RESTAURANT, MEDICAL
    val userId: String = "",
    val userName: String = "",
    val userPhone: String = "",
    val bookingId: String = "",
    val rating: Float = 5.0f,
    val qualityRating: Float = 5.0f,
    val speedRating: Float = 5.0f,
    val professionalismRating: Float = 5.0f,
    val priceFairnessRating: Float = 5.0f,
    val photoUrl: String = "",
    val comment: String = "",
    val isApproved: Boolean = true,
    val timestamp: Long = System.currentTimeMillis(),
    val reply: String = "",
    val replyTimestamp: Long? = null,
    val helpfulCount: Int = 0,
    val unhelpfulCount: Int = 0,
    val helpfulUserIds: List<String> = emptyList(),
    val unhelpfulUserIds: List<String> = emptyList(),
    val isReported: Boolean = false,
    val reportReason: String = ""
) {
    val providerId: String get() = targetId
    val customerName: String get() = userName
    val authorName: String get() = userName
    val authorPhone: String get() = userPhone
    val dateTimestamp: Long get() = timestamp

    fun toReviewEntity(): com.example.domain.entities.RatingReviewEntity {
        return com.example.domain.entities.RatingReviewEntity(
            id = id,
            targetId = targetId,
            authorName = userName.ifBlank { "عميل" },
            authorPhone = userPhone,
            rating = rating.toDouble(),
            comment = comment,
            dateTimestamp = timestamp
        )
    }

    fun toReview(): Review {
        return Review(
            id = id,
            shopId = targetId,
            userId = userId,
            userName = userName,
            rating = rating.toInt(),
            text = comment,
            timestamp = timestamp
        )
    }
}
