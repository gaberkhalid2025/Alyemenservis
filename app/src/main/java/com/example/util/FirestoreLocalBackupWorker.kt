package com.example.util

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * WorkManager worker that performs daily local encrypted backups (AES-256)
 * of offline Firestore persistent documents.
 */
class FirestoreLocalBackupWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            Log.d(TAG, "Starting daily local encrypted backup of Firestore cache...")
            val db = FirebaseFirestore.getInstance()

            val backupSb = StringBuilder()
            backupSb.append("{\n\"timestamp\": \"${System.currentTimeMillis()}\",\n\"collections\": {\n")

            val collectionsToBackup = listOf(
                "providers", "stores", "properties", "bookings",
                "registered_users", "settings", "chats"
            )

            var isFirstCol = true
            for (colName in collectionsToBackup) {
                try {
                    val snapshot = db.collection(colName).get().await()
                    if (!isFirstCol) backupSb.append(",\n")
                    isFirstCol = false

                    backupSb.append("  \"$colName\": [")
                    var isFirstDoc = true
                    for (doc in snapshot.documents) {
                        if (!isFirstDoc) backupSb.append(",")
                        isFirstDoc = false
                        val dataJson = doc.data?.entries?.joinToString(prefix = "{", postfix = "}") {
                            "\"${it.key}\": \"${it.value.toString().replace("\"", "\\\"")}\""
                        } ?: "{}"
                        backupSb.append(dataJson)
                    }
                    backupSb.append("]")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed backing up collection $colName", e)
                }
            }

            backupSb.append("\n}\n}")

            val rawBackupString = backupSb.toString()
            val encryptedData = SecurityCryptoUtils.encrypt(rawBackupString)

            val backupDir = File(context.filesDir, "encrypted_backups")
            if (!backupDir.exists()) {
                backupDir.mkdirs()
            }

            val timestampStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val backupFile = File(backupDir, "wam_firestore_backup_$timestampStr.enc")

            FileOutputStream(backupFile).use { fos ->
                fos.write(encryptedData.toByteArray(Charsets.UTF_8))
            }

            Log.d(TAG, "Daily encrypted backup created successfully: ${backupFile.absolutePath}, size=${backupFile.length()} bytes")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error performing Firestore local backup", e)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "FirestoreBackupWorker"
        private const val WORK_NAME = "DailyFirestoreLocalEncryptedBackup"

        fun schedulePeriodicBackup(context: Context) {
            val backupRequest = PeriodicWorkRequestBuilder<FirestoreLocalBackupWorker>(24, TimeUnit.HOURS)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                androidx.work.ExistingPeriodicWorkPolicy.KEEP,
                backupRequest
            )
            Log.d(TAG, "Scheduled daily 24-hour encrypted local backup worker.")
        }
    }
}
