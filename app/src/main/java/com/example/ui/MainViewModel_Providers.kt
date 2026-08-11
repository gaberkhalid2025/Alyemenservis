package com.example.ui

import com.example.data.*
import java.util.UUID

fun MainViewModel.submitRating(targetId: String, rating: Float, comment: String) {
    val newRating = RatingEntity(
        id = UUID.randomUUID().toString(),
        targetId = targetId,
        rating = rating,
        comment = comment,
        timestamp = System.currentTimeMillis()
    )
    _ratings.value = _ratings.value + newRating
}
