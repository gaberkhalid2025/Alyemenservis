package com.example.data.repositories

import com.example.data.LocalAppCacheManager
import com.example.ui.screens.dashboard.viewmodels.JobPostItem
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class JobRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val cacheManager: LocalAppCacheManager
) {
    fun getJobs(ownerId: String): Flow<List<JobPostItem>> = callbackFlow {
        val listener = firestore.collection("job_postings")
            .whereEqualTo("ownerId", ownerId)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val list = snapshot.documents.mapNotNull { doc ->
                    JobPostItem(
                        id = doc.id,
                        title = doc.getString("title") ?: "",
                        companyName = doc.getString("companyName") ?: "",
                        salary = doc.getString("salary") ?: "",
                        requirements = doc.getString("requirements") ?: "",
                        applicantsCount = doc.getLong("applicantsCount")?.toInt() ?: 0
                    )
                }
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    suspend fun postJob(ownerId: String, job: JobPostItem): Result<String> {
        return try {
            val data = mapOf(
                "id" to job.id,
                "ownerId" to ownerId,
                "title" to job.title,
                "companyName" to job.companyName,
                "salary" to job.salary,
                "requirements" to job.requirements,
                "applicantsCount" to job.applicantsCount,
                "createdAt" to System.currentTimeMillis()
            )
            firestore.collection("job_postings").document(job.id).set(data).await()
            Result.success(job.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteJob(id: String): Result<Unit> {
        return try {
            firestore.collection("job_postings").document(id).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
