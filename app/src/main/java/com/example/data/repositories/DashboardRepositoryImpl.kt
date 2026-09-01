package com.example.data.repositories

import android.content.Context
import com.example.data.utils.AppResult
import com.example.domain.entities.DashboardStatsEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class DashboardRepositoryImpl(private val context: Context? = null) : IDashboardRepository {
    override fun getDashboardStats(ownerId: String, userType: String): Flow<DashboardStatsEntity> = flowOf(DashboardStatsEntity())
    override suspend fun getDashboardStats(ownerId: String): AppResult<DashboardStatsEntity> = AppResult.success(DashboardStatsEntity())
}
