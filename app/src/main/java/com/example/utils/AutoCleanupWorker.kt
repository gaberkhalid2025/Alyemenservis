package com.example.utils

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class AutoCleanupWorker(
    ctx: Context,
    params: WorkerParameters
) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        return try {
            val daysToKeep = inputData.getInt(KEY_DAYS_TO_KEEP, 30).coerceAtLeast(1)
            val cutoffTime = System.currentTimeMillis() - (daysToKeep * 24L * 60L * 60L * 1000L)
            val db = FirebaseFirestore.getInstance()
            val snapshot = db.collection("notifications")
                .whereLessThan("timestamp", cutoffTime)
                .limit(450)
                .get()
                .await()

            if (!snapshot.isEmpty) {
                snapshot.documents.chunked(450).forEach { chunk ->
                    val batch = db.batch()
                    chunk.forEach { doc -> batch.delete(doc.reference) }
                    batch.commit().await()
                }
            }
            Result.success()
        } catch (e: Exception) {
            Log.e("AutoCleanupWorker", "Error cleaning expired notifications", e)
            Result.retry()
        }
    }

    companion object {
        const val KEY_DAYS_TO_KEEP = "days_to_keep"
        const val UNIQUE_WORK_NAME = "auto_cleanup_notifications"
    }
}
