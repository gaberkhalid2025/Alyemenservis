package com.example.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * 🔄 SyncManager - جدولة المزامنة الدورية الذكية لتوفير استهلاك بيانات الإنترنت والبطارية
 * Utilizes WorkManager with connectivity constraints and exponential backoff retry.
 */
class SyncManager(private val context: Context) {

    fun schedulePeriodicSync() {
        // Run sync only when connected to network to preserve offline state and battery
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        // 2 hours interval with Exponential backoff retry starting at 15 minutes
        val syncWorkRequest = PeriodicWorkRequestBuilder<SyncWorker>(2, TimeUnit.HOURS)
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                15,
                TimeUnit.MINUTES
            )
            .build()

        WorkManager.getInstance(context).enqueue(syncWorkRequest)
    }
}
