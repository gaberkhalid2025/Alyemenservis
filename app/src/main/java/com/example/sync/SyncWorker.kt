package com.example.sync

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.utils.FullSyncManager
import com.example.utils.OfflineQueueManager
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 🔄 SyncWorker - عامل مزامنة البيانات المحلية وسجلات الحجوزات في الخلفية بذكاء لتوفير الإنترنت
 */
class SyncWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface SyncWorkerEntryPoint {
        fun fullSyncManager(): FullSyncManager
        fun offlineQueueManager(): OfflineQueueManager
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        return@withContext try {
            val (syncManager, offlineQueue) = try {
                val entryPoint = EntryPointAccessors.fromApplication(
                    applicationContext,
                    SyncWorkerEntryPoint::class.java
                )
                entryPoint.fullSyncManager() to entryPoint.offlineQueueManager()
            } catch (e: Throwable) {
                FullSyncManager(applicationContext) to OfflineQueueManager(applicationContext)
            }

            // 1. سحب أحدث الإعدادات من السحابة إلى الكاش المحلي بأمان (دون الكتابة الفوقية على Firestore)
            syncManager.pullRemoteSettingsToLocalCache()

            // 2. معالجة طابور وضع عدم الاتصال (Offline Queue) والانتظار الفعلي حتى اكتمال الإرسال
            val queueSuccess = offlineQueue.processQueueSuspend()

            if (!queueSuccess && runAttemptCount < MAX_RETRY_ATTEMPTS) {
                Result.retry()
            } else {
                Result.success()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Background sync worker failed: ${e.message}", e)
            if (runAttemptCount < MAX_RETRY_ATTEMPTS) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }

    companion object {
        private const val TAG = "SyncWorker"
        private const val MAX_RETRY_ATTEMPTS = 3
        const val WORK_NAME = "periodic_sync_work"
    }
}

