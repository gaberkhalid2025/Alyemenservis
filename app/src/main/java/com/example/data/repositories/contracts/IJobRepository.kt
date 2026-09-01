package com.example.data.repositories.contracts

import com.example.data.JobEntity
import com.example.data.utils.AppResult
import kotlinx.coroutines.flow.Flow

interface IJobRepository {
    fun clearListeners()
    fun observeAllJobs(): Flow<List<JobEntity>>
    fun observeJobsByCity(cityId: String): Flow<List<JobEntity>>
    suspend fun applyForJob(jobId: String, applicantName: String, applicantPhone: String, notes: String = ""): AppResult<String>
    suspend fun saveOrUpdateJob(job: JobEntity): AppResult<JobEntity>
    suspend fun deleteJob(jobId: String): AppResult<Unit>
}
