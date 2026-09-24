package com.example.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.utils.OfflineQueueManager
import com.example.utils.FullSyncManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 🔄 SyncWorker - عامل مزامنة البيانات المحلية وسجلات الحجوزات في الخلفية بذكاء لتوفير الإنترنت
 */
class SyncWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        return@withContext try {
            val syncManager = FullSyncManager(applicationContext)
            val offlineQueue = OfflineQueueManager(applicationContext)

            // 1. مزامنة الإعدادات الشاملة
            syncManager.syncAllSettings()

            // 2. معالجة طابور وضع عدم الاتصال (Offline Queue)
            offlineQueue.processQueue()

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }

    companion object {
        const val WORK_NAME = "PeriodicSyncWork"
    }
}

