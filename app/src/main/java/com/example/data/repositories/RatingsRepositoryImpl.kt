package com.example.data.repositories

import android.content.Context
import com.example.data.utils.AppResult
import com.example.domain.entities.RatingReviewEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class RatingsRepositoryImpl(private val context: Context? = null) : IRatingsRepository {
    override fun getTargetRatings(targetId: String): Flow<List<RatingReviewEntity>> = flowOf(emptyList())
    override suspend fun addRatingReply(ratingId: String, reply: String): AppResult<Unit> = AppResult.success(Unit)
}
