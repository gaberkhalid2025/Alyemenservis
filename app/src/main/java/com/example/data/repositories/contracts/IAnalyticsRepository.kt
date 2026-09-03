package com.example.data.repositories.contracts

import com.example.data.utils.AppResult

interface IAnalyticsRepository {
    suspend fun logEvent(eventName: String, params: Map<String, Any> = emptyMap()): AppResult<Unit>
    suspend fun logScreenView(screenName: String): AppResult<Unit>
    suspend fun getPerformanceStats(): AppResult<Map<String, Any>>
}
