package com.example.data.repositories

import com.example.data.utils.AppResult
import com.example.domain.entities.DashboardStatsEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

interface IDashboardRepository {
    fun getDashboardStats(ownerId: String, userType: String = ""): Flow<DashboardStatsEntity> = flowOf(DashboardStatsEntity())
    suspend fun getDashboardStats(ownerId: String): AppResult<DashboardStatsEntity> = AppResult.success(DashboardStatsEntity())
}
