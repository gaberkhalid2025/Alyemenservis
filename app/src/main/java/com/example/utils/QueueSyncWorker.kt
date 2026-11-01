package com.example.utils

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface QueueWorkerEntryPoint {
    fun offlineQueueManager(): OfflineQueueManager
}

class QueueSyncWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        val q = EntryPointAccessors.fromApplication(
            applicationContext,
            QueueWorkerEntryPoint::class.java
        ).offlineQueueManager()
        return if (q.processQueueSuspend()) Result.success() else Result.retry()
    }
}
