package com.example.data.repositories.impl

import android.content.Context
import android.util.Log
import com.example.data.JobEntity
import com.example.data.repositories.contracts.IJobRepository
import com.example.data.utils.AppError
import com.example.data.utils.AppResult
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class JobRepositoryImpl(
    private val context: Context?,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : IJobRepository {

    private val listeners = mutableListOf<ListenerRegistration>()
    private val jobsCollection = firestore.collection("jobs")
    private val applicationsCollection = firestore.collection("job_applications")

    companion object {
        private const val TAG = "JobRepositoryImpl"
    }

    override fun clearListeners() {
        try {
            listeners.forEach { it.remove() }
            listeners.clear()
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing listeners", e)
        }
    }

    override fun observeAllJobs(): Flow<List<JobEntity>> = callbackFlow {
        val listener = jobsCollection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Error observing jobs", error)
                return@addSnapshotListener
            }
            val list = snapshot?.documents?.mapNotNull { doc ->
                doc.toObject(JobEntity::class.java)?.copy(id = doc.id)
            } ?: emptyList()
            trySend(list)
        }
        listeners.add(listener)

        awaitClose {
            listener.remove()
        }
    }.flowOn(Dispatchers.IO)

    override fun observeJobsByCity(cityId: String): Flow<List<JobEntity>> = callbackFlow {
        val listener = jobsCollection
            .whereEqualTo("cityId", cityId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error observing jobs for city $cityId", error)
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(JobEntity::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                trySend(list)
            }
        listeners.add(listener)

        awaitClose {
            listener.remove()
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun applyForJob(jobId: String, applicantName: String, applicantPhone: String, notes: String): AppResult<String> = withContext(Dispatchers.IO) {
        try {
            val appId = applicationsCollection.document().id
            val appData = mapOf(
                "id" to appId,
                "jobId" to jobId,
                "applicantName" to applicantName,
                "applicantPhone" to applicantPhone,
                "notes" to notes,
                "createdAt" to System.currentTimeMillis()
            )
            applicationsCollection.document(appId).set(appData).await()
            Result.success(appId)
        } catch (e: Exception) {
            Log.e(TAG, "Error applying for job $jobId", e)
            Result.failure(AppError.DatabaseError(e.localizedMessage ?: "فشل التقديم على الوظيفة"))
        }
    }

    override suspend fun saveOrUpdateJob(job: JobEntity): AppResult<JobEntity> = withContext(Dispatchers.IO) {
        try {
            val docId = job.id.ifBlank { jobsCollection.document().id }
            val finalJob = job.copy(id = docId)

            jobsCollection.document(docId).set(finalJob, SetOptions.merge()).await()
            Result.success(finalJob)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving job", e)
            Result.failure(AppError.DatabaseError(e.localizedMessage ?: "فشل حفظ إعلان الوظيفة"))
        }
    }

    override suspend fun deleteJob(jobId: String): AppResult<Unit> = withContext(Dispatchers.IO) {
        try {
            jobsCollection.document(jobId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting job $jobId", e)
            Result.failure(AppError.DatabaseError(e.localizedMessage ?: "فشل حذف إعلان الوظيفة"))
        }
    }
}
