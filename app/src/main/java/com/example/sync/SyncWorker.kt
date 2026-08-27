package com.example.sync

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.util.SyncManager as RealSyncManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 🔄 SyncWorker - عامل مزامنة البيانات المحلية وسجلات الحجوزات في الخلفية بذكاء لتوفير الإنترنت
 * Runs asynchronously on a Coroutine threadpool with full backoff retry support.
 */
class SyncWorker(context: Context, workerParams: WorkerParameters) : CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Log.d("SyncWorker", "Executing scheduled background data sync...")
            val syncEngine = RealSyncManager(applicationContext)
            
            // Execute deep background sync
            val success = syncEngine.syncAllSettings()
            
            if (success) {
                Log.d("SyncWorker", "Background sync completed successfully!")
                Result.success()
            } else {
                Log.w("SyncWorker", "Background sync failed. Scheduling retry with exponential backoff...")
                Result.retry()
            }
        } catch (e: Exception) {
            Log.e("SyncWorker", "Error executing background sync: ${e.message}", e)
            Result.retry()
        }
    }
}
