package com.example.data.repositories.impl

import com.google.firebase.firestore.FirebaseFirestore
import com.example.data.repositories.contracts.IAnalyticsRepository
import com.example.data.utils.AppError
import com.example.data.utils.AppResult
import kotlinx.coroutines.tasks.await

class AnalyticsRepositoryImpl(
    private val firestore: FirebaseFirestore
) : IAnalyticsRepository {

    override suspend fun logEvent(eventName: String, params: Map<String, Any>): AppResult<Unit> {
        return try {
            val eventData = hashMapOf(
                "eventName" to eventName,
                "timestamp" to System.currentTimeMillis(),
                "params" to params
            )
            firestore.collection("analytics_events")
                .add(eventData)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(AppError.DatabaseError(e.localizedMessage ?: "فشل تسجيل الحدث"))
        }
    }

    override suspend fun logScreenView(screenName: String): AppResult<Unit> {
        return logEvent("screen_view", mapOf("screen_name" to screenName))
    }

    override suspend fun getPerformanceStats(): AppResult<Map<String, Any>> {
        return try {
            val querySnapshot = firestore.collection("analytics_events")
                .limit(100)
                .get()
                .await()
            val totalEvents = querySnapshot.size().toLong()
            Result.success(mapOf("total_recorded_events" to totalEvents))
        } catch (e: Exception) {
            Result.failure(AppError.DatabaseError(e.localizedMessage ?: "فشل استرجاع بيانات الأداء"))
        }
    }
}
