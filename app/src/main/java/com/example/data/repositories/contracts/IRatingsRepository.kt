package com.example.data.repositories.contracts

import com.example.data.utils.AppResult
import com.example.domain.entities.RatingReviewEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

interface IRatingsRepository {
    fun getTargetRatings(targetId: String): Flow<List<RatingReviewEntity>> = flowOf(emptyList())
    suspend fun addRatingReply(ratingId: String, reply: String): AppResult<Unit> = AppResult.success(Unit)
}
