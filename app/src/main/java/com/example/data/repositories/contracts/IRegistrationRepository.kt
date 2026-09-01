package com.example.data.repositories.contracts

import com.example.domain.entities.JoinStatusEntity
import com.example.domain.entities.RegistrationEntity
import com.example.data.utils.AppResult
import kotlinx.coroutines.flow.Flow

interface IRegistrationRepository {
    suspend fun registerClient(client: RegistrationEntity.Client): AppResult<String>
    suspend fun registerProvider(provider: RegistrationEntity.Provider): AppResult<String>
    suspend fun registerStore(store: RegistrationEntity.Store): AppResult<String>
    suspend fun registerRestaurant(restaurant: RegistrationEntity.Restaurant): AppResult<String>
    suspend fun registerMedicalCenter(medical: RegistrationEntity.MedicalCenter): AppResult<String>
    suspend fun registerProperty(property: RegistrationEntity.Property): AppResult<String>
    suspend fun registerJob(job: RegistrationEntity.Job): AppResult<String>
    fun getJoinStatusFlow(phoneNumber: String): Flow<JoinStatusEntity?>
}
