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

    override suspend fun getForegroundInfo(): androidx.work.ForegroundInfo {
        val nm = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        if (android.os.Build.VERSION.SDK_INT >= 26) {
            nm.createNotificationChannel(
                android.app.NotificationChannel(
                    "queue_sync",
                    "مزامنة الطلبات",
                    android.app.NotificationManager.IMPORTANCE_LOW
                )
            )
        }
        val n = androidx.core.app.NotificationCompat.Builder(applicationContext, "queue_sync")
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentTitle("جارٍ إرسال طلباتك المحفوظة")
            .setOngoing(true)
            .build()
        return androidx.work.ForegroundInfo(7101, n)
    }

    override suspend fun doWork(): Result {
        val q = EntryPointAccessors.fromApplication(
            applicationContext,
            QueueWorkerEntryPoint::class.java
        ).offlineQueueManager()
        return if (q.processQueueSuspend()) Result.success() else Result.retry()
    }
}

